package com.mycelium.wapi.api.response;

public enum WarningKind {
   // used for version updates with no critical fixes
   INFO,

   // used for version update with critical fixes or other situations that might lead to financial loss
   WARN,

   // only used for external services or other non-core functionality, which will lead definitely to financial loss
   BLOCK
}
