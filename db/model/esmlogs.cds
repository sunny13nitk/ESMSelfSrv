using {cuid} from '@sap/cds/common';

namespace db.esmlogs;


/*
--- Cannot be inserted Directly via OData - Managed via Logging Event
*/
@Capabilities.Insertable: false
@Capabilities.Updatable : false
entity esmapplogs : cuid {
    username  : String(50);
    casetype  : String(50);
    timestamp : Timestamp;
    status    : String(50);
    msgtype   : String(50);
    objectid  : String(50);
    message   : String(1000);
}

@Capabilities.Insertable: false
@Capabilities.Updatable : false
@Capabilities.Deletable : false
entity baseconfig {
    key caseTypeEnum       : String(50);
        casetype           : String(10);
        appNotesTypes      : String(50);
        statusSchema       : String(5);
        replyNoteType      : String(50);
        catgRankEnabled    : Boolean;
        catgsranksonlyShow : Boolean;
        confirmStatus      : String(20);
        fragmentHead       : String(10);
        fragmentTitle      : String(10);
        fragmentFooter     : String(10);
        caseFormView       : String(50);
        svydes             : String(50);
        svysrv             : String(50);
        logouturl          : String(255);
}

@Capabilities.Insertable: false
@Capabilities.Updatable : false
@Capabilities.Deletable : false
entity lobcatgsranks : cuid {
    casetype : String(10);
    catg     : String(200);
    rank     : Int16;

}

@Capabilities.Insertable: false
@Capabilities.Updatable : false
@Capabilities.Deletable : false
entity catgtemplates : cuid {
    catgU         : String(255);
    questionnaire : LargeString;


}

@Capabilities.Insertable: false
@Capabilities.Updatable : false
@Capabilities.Deletable : false
entity statustrans : cuid {
    casetype        : String(10);
    fromStatus      : String(30);
    toStatus        : String(30);
    caseEditAllowed : Boolean;
    confirmAllowed  : Boolean;
}
