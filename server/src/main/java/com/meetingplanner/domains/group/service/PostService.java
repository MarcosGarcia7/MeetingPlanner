package com.meetingplanner.domains.group.service;

import com.meetingplanner.common.api.CursorCodec;
import com.meetingplanner.common.api.dto.CommentsEnabledRequest;
import com.meetingplanner.common.api.dto.CursorPage;
import com.meetingplanner.common.api.dto.PostCommentCreateRequest;
import com.meetingplanner.common.api.dto.PostCommentResponse;
import com.meetingplanner.common.api.dto.PostCreateRequest;
import com.meetingplanner.common.api.dto.PostResponse;
import com.meetingplanner.common.api.exceptions.ResourceNotFoundException;
import com.meetingplanner.domains.group.model.GroupEntity;
import com.meetingplanner.domains.group.model.GroupMemberEntity;
import com.meetingplanner.domains.group.model.GroupPostCommentEntity;
import com.meetingplanner.domains.group.model.GroupPostEntity;
import com.meetingplanner.domains.group.repository.GroupMemberRepository;
import com.meetingplanner.domains.group.repository.GroupPostCommentRepository;
import com.meetingplanner.domains.group.repository.GroupPostRepository;
import com.meetingplanner.domains.group.repository.GroupRepository;
import com.meetingplanner.domains.notification.service.NotificationService;
import com.meetingplanner.domains.user.model.UserEntity;
import com.meetingplanner.domains.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class PostService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPostRepository postRepository;
    private final GroupPostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PostService(GroupRepository groupRepository,
                       GroupMemberRepository groupMemberRepository,
                       GroupPostRepository postRepository,
                       GroupPostCommentRepository commentRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Value("${app.base-url:https://meetingplanner.example.com}")
    private String appBaseUrl;

    @Transactional
    public PostResponse create(long userId, long groupId, PostCreateRequest request) {
        GroupEntity group = requireGroup(groupId);
        UserEntity author = requireUser(userId);
        ensureCanPost(author, group);

        GroupPostEntity post = new GroupPostEntity();
        post.setGroup(group);
        post.setAuthor(author);
        post.setBody(request.body().trim());
        post.setCommentsEnabled(request.commentsEnabled());
        postRepository.save(post);

        notifyGroupMembers(group, post, author);

        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostResponse> list(long userId, long groupId, String cursor, int size) {
        GroupEntity group = requireGroup(groupId);
        requireMember(group, userId);

        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupPostEntity> rows = postRepository.listByGroup(
                groupId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupPostEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<PostResponse> items = page.stream().map(PostResponse::from).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getCreatedAt(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listRecent(long userId, String cursor, int size) {
        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupPostEntity> rows = postRepository.listRecentForMember(
                userId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupPostEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<PostResponse> items = page.stream().map(PostResponse::from).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getCreatedAt(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostCommentResponse> listComments(long userId, long groupId, long postId, String cursor, int size) {
        GroupEntity group = requireGroup(groupId);
        requireMember(group, userId);
        requirePost(postId);

        int limit = CursorCodec.clampSize(size);
        CursorCodec.Cursor decoded = CursorCodec.decode(cursor);
        List<GroupPostCommentEntity> rows = commentRepository.listByPost(
                postId,
                decoded == null ? null : decoded.time(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<GroupPostCommentEntity> page = hasMore ? rows.subList(0, limit) : rows;
        List<PostCommentResponse> items = page.stream().map(PostCommentResponse::from).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? CursorCodec.encode(page.getLast().getCreatedAt(), page.getLast().getId())
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public PostResponse get(long userId, long groupId, long postId) {
        GroupEntity group = requireGroup(groupId);
        requireMember(group, userId);
        return PostResponse.from(requirePost(postId));
    }

    @Transactional
    public void delete(long userId, long postId) {
        GroupPostEntity post = requirePost(postId);
        if (!canModerate(userId, post)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this post");
        }
        commentRepository.deleteByPostId(post.getId());
        postRepository.delete(post);
    }

    @Transactional
    public PostCommentResponse addComment(long userId, long postId, PostCommentCreateRequest request) {
        GroupPostEntity post = requirePost(postId);
        GroupEntity group = post.getGroup();
        requireMember(group, userId);

        if (!isCommentsEnabled(post)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Comments are disabled");
        }

        UserEntity author = requireUser(userId);
        GroupPostCommentEntity comment = new GroupPostCommentEntity();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());
        commentRepository.save(comment);

        if (!post.getAuthor().getId().equals(userId)) {
            String link = appBaseUrl + "/groups/" + group.getId() + "/posts/" + post.getId();
            notificationService.push(post.getAuthor().getId(), "POST_COMMENT", "post_comments", "POST_COMMENT", Map.of(
                    "name", displayName(post.getAuthor()),
                    "author", displayName(author),
                    "body", snippet(comment.getBody()),
                    "link", link));
        }

        return PostCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(long userId, long commentId) {
        GroupPostCommentEntity comment = requireComment(commentId);
        if (!canModerate(userId, comment.getPost())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public void setCommentsEnabled(long userId, long postId, CommentsEnabledRequest request) {
        GroupPostEntity post = requirePost(postId);
        if (!post.getGroup().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can toggle comments");
        }
        post.setCommentsEnabled(request.commentsEnabled());
        postRepository.save(post);
    }

    private boolean isCommentsEnabled(GroupPostEntity post) {
        return post.getCommentsEnabled() != null
                ? post.getCommentsEnabled()
                : post.getGroup().isCommentsEnabled();
    }

    private boolean canModerate(long userId, GroupPostEntity post) {
        GroupEntity group = post.getGroup();
        if (group.getOwner().getId().equals(userId)) {
            return true;
        }
        if (post.getAuthor().getId().equals(userId)) {
            return true;
        }
        return groupMemberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(GroupMemberEntity::isAdmin)
                .orElse(false);
    }

    private void ensureCanPost(UserEntity author, GroupEntity group) {
        if (group.getOwner().getId().equals(author.getId())) {
            return;
        }
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(group.getId(), author.getId())
                .map(GroupMemberEntity::isAdmin)
                .orElse(false);
        if (isAdmin) {
            return;
        }
        if (group.isMembersCanPost() && isMember(group, author.getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to post in this group");
    }

    private boolean isMember(GroupEntity group, long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId);
    }

    private void requireMember(GroupEntity group, long userId) {
        if (group.getOwner().getId().equals(userId)) {
            return;
        }
        if (!isMember(group, userId)) {
            throw new ResourceNotFoundException("Group not found");
        }
    }

    private void notifyGroupMembers(GroupEntity group, GroupPostEntity post, UserEntity author) {
        String link = appBaseUrl + "/groups/" + group.getId() + "/posts/" + post.getId();
        for (GroupMemberEntity member : groupMemberRepository.findByGroupId(group.getId())) {
            if (member.getUser().getId().equals(author.getId())) {
                continue;
            }
            notificationService.push(member.getUser().getId(), "GROUP_POST", "group_posts", "GROUP_POST", Map.of(
                    "name", displayName(member.getUser()),
                    "author", displayName(author),
                    "group", group.getGroupName(),
                    "body", snippet(post.getBody()),
                    "link", link));
        }
    }

    private GroupEntity requireGroup(long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    private GroupPostEntity requirePost(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private GroupPostCommentEntity requireComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private UserEntity requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static String snippet(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
    }

    private static String displayName(UserEntity user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getUsername();
    }
}