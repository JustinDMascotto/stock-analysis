package org.bde.chart.generator.service.ruleengines;

import lombok.Getter;


public enum RuleEngineOutput
{
    BUY( "Buy/" ),
    SELL( "Sell/" ),
    DO_NOTHING( "Nothing/" );

    @Getter
    private final String subfolderName;

    RuleEngineOutput( final String subfolderName )
    {
        this.subfolderName = subfolderName;
    }
}
