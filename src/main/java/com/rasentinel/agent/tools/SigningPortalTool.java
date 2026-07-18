package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.SigningPortalSession;

public interface SigningPortalTool {
    SigningPortalSession getSessionStatus(String raId);
}
