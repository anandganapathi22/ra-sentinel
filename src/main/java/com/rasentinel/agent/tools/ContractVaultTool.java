package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.ContractVaultStatus;

public interface ContractVaultTool {
    ContractVaultStatus getAgreementStatus(String raId);
}
