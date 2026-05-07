package com.company.crms.contract;

import com.company.crms.common.exception.BizException;
import com.company.crms.contract.enums.ContractStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractStatusMachineTest {

    @Test
    void draft_can_to_effective_or_terminated() {
        assertDoesNotThrow(() -> ContractStatus.assertTransition(ContractStatus.DRAFT, ContractStatus.EFFECTIVE));
        assertDoesNotThrow(() -> ContractStatus.assertTransition(ContractStatus.DRAFT, ContractStatus.TERMINATED));
    }

    @Test
    void draft_cannot_jump_to_completed() {
        assertThrows(BizException.class,
                () -> ContractStatus.assertTransition(ContractStatus.DRAFT, ContractStatus.COMPLETED));
    }

    @Test
    void terminated_is_terminal() {
        for (ContractStatus s : ContractStatus.values()) {
            if (s == ContractStatus.TERMINATED) continue;
            assertThrows(BizException.class,
                    () -> ContractStatus.assertTransition(ContractStatus.TERMINATED, s));
        }
    }

    @Test
    void effective_can_to_completed_or_terminated_or_expired() {
        assertDoesNotThrow(() -> ContractStatus.assertTransition(ContractStatus.EFFECTIVE, ContractStatus.COMPLETED));
        assertDoesNotThrow(() -> ContractStatus.assertTransition(ContractStatus.EFFECTIVE, ContractStatus.TERMINATED));
        assertDoesNotThrow(() -> ContractStatus.assertTransition(ContractStatus.EFFECTIVE, ContractStatus.EXPIRED));
    }

    @Test
    void same_status_rejected() {
        assertThrows(BizException.class,
                () -> ContractStatus.assertTransition(ContractStatus.DRAFT, ContractStatus.DRAFT));
    }
}
