package com.meetingplanner.it;

import com.meetingplanner.domains.group.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupJoinConcurrencyIT extends AbstractPostgresIT {

    private static final int ROUNDS = 10;

    @Autowired
    private GroupService groupService;

    @Test
    void duplicateConcurrentJoinsCreateOneMembershipWithoutErrors() throws Exception {
        long owner = insertUser("owner");
        long joiner = insertUser("joiner");

        for (int round = 0; round < ROUNDS; round++) {
            long groupId = insertGroup("open-" + round, owner, false);

            List<Outcome<Object>> outcomes = runConcurrently(
                    () -> { groupService.join(joiner, groupId); return null; },
                    () -> { groupService.join(joiner, groupId); return null; });

            for (Outcome<Object> outcome : outcomes) {
                assertTrue(outcome.succeeded(), () -> "join failed: " + outcome.error());
            }
            assertEquals(1, count("select count(*) from group_members where group_id = ? and user_id = ?",
                    groupId, joiner));
        }
    }
}
