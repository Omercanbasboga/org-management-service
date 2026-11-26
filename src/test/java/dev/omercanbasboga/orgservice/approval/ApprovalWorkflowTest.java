package dev.omercanbasboga.orgservice.approval;

import dev.omercanbasboga.orgservice.common.ForbiddenException;
import org.junit.jupiter.api.Test;

import static dev.omercanbasboga.orgservice.approval.ApprovalStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalWorkflowTest {

    @Test
    void advisorThenAdmin() {
        ApprovalStatus s = ApprovalWorkflow.firstStep(true);
        assertThat(s).isEqualTo(PENDING_ADVISOR);
        s = ApprovalWorkflow.approve(s, Approver.ADVISOR);
        assertThat(s).isEqualTo(PENDING_ADMIN);
        assertThat(ApprovalWorkflow.approve(s, Approver.ADMIN)).isEqualTo(APPROVED);
    }

    @Test
    void organizationWithoutAdvisorStartsAtTheAdmin() {
        assertThat(ApprovalWorkflow.firstStep(false)).isEqualTo(PENDING_ADMIN);
    }

    @Test
    void nobodySignsOutOfTurn() {
        assertThatThrownBy(() -> ApprovalWorkflow.approve(PENDING_ADVISOR, Approver.ADMIN))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("advisor");
        assertThatThrownBy(() -> ApprovalWorkflow.approve(PENDING_ADMIN, Approver.ADVISOR))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("admin");
        assertThatThrownBy(() -> ApprovalWorkflow.approve(PENDING_ADMIN, Approver.NOBODY))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void eitherSignerCanRejectAtTheirStep() {
        assertThat(ApprovalWorkflow.reject(PENDING_ADVISOR, Approver.ADVISOR)).isEqualTo(REJECTED);
        assertThat(ApprovalWorkflow.reject(PENDING_ADMIN, Approver.ADMIN)).isEqualTo(REJECTED);
    }

    @Test
    void finishedRequestsStayFinished() {
        assertThatThrownBy(() -> ApprovalWorkflow.approve(APPROVED, Approver.ADMIN))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("approved");
        assertThatThrownBy(() -> ApprovalWorkflow.reject(REJECTED, Approver.ADMIN))
                .isInstanceOf(IllegalStateException.class);
    }
}
