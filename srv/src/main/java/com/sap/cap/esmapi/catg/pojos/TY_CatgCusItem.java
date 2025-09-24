package com.sap.cap.esmapi.catg.pojos;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TY_CatgCusItem
{
    @CsvBindByPosition(position = 0)
    private String caseTypeEnum;
    @CsvBindByPosition(position = 1)
    private String caseType;
    @CsvBindByPosition(position = 2)
    private String appNoteTypes;
    @CsvBindByPosition(position = 3)
    private String statusSchema;
    @CsvBindByPosition(position = 4)
    private String replyNoteType;
    @CsvBindByPosition(position = 5)
    private Boolean catgRankEnabled;
    @CsvBindByPosition(position = 6)
    private Boolean catgsranksonlyShow;
    @CsvBindByPosition(position = 7)
    private String confirmStatus;
    @CsvBindByPosition(position = 8)
    private String fragmentHead;
    @CsvBindByPosition(position = 9)
    private String fragmentTitle;
     @CsvBindByPosition(position = 10)
    private String fragmentFooter;
    @CsvBindByPosition(position = 11)
    private String caseFormView;
    @CsvBindByPosition(position = 12)
    private String svydes;
    @CsvBindByPosition(position = 13)
    private String svysrv;
    @CsvBindByPosition(position = 14)
    private String logoutUrl;

}
