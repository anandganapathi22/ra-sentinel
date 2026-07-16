package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.RmsRentalAgreement;

public interface RmsTool {
    RmsRentalAgreement getRentalAgreement(String raId);
}
