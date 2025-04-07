using {cuid} from '@sap/cds/common';

namespace db.esmlogs;


/*
--- Cannot be inserted Directly via OData - Managed via Logging Event
*/
@Capabilities.Insertable: false
@Capabilities.Updatable : false
entity esmapplogs : cuid {
    username : String(50);
    casetype : String(50);
    timestamp: Timestamp;
    status   : String(50);
    msgtype  : String(50);
    objectid : String(50);
    message  : String(1000);
}



