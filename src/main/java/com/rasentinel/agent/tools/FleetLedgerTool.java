package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.FleetLedgerAgreement;

public interface FleetLedgerTool {
    FleetLedgerAgreement getRentalAgreement(String raId);
}
