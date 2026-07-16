package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.TasAgreementStatus;

public interface TasTool {
    TasAgreementStatus getAgreementStatus(String raId);
}
