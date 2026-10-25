package com.meetingplanner.common.api.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.meetingplanner.common.api.dto.DtoValidation.violatedPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupUpdateRequestTest {

    private static GroupUpdateRequest withName(String groupName) {
        return new GroupUpdateRequest(groupName, null, null, null, null);
    }

    @Test
    void nameChangePasses() {
        assertTrue(violatedPaths(withName("Book club")).isEmpty());
    }

    @Test
    void omittedNamePasses() {
        assertTrue(violatedPaths(withName(null)).isEmpty());
    }

    @Test
    void blankNameFails() {
        assertEquals(Set.of("groupName"), violatedPaths(withName("")));
        assertEquals(Set.of("groupName"), violatedPaths(withName("   ")));
        assertEquals(Set.of("groupName"), violatedPaths(withName("\t\n")));
    }

    @Test
    void oversizedNameFails() {
        assertEquals(Set.of("groupName"), violatedPaths(withName("a".repeat(51))));
    }
}
