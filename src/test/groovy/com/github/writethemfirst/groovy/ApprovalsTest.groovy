package com.github.writethemfirst.groovy
import org.junit.Test

import com.github.writethemfirst.approvals.Approvals

import groovy.transform.TypeChecked

@TypeChecked
class ApprovalsTest {
    @Test
    public void approvalsShouldVerify() {
        Approvals.verify([])
    }
}
