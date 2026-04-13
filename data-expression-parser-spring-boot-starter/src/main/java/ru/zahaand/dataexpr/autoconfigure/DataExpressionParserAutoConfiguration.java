package ru.zahaand.dataexpr.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

@AutoConfiguration
public class DataExpressionParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExpressionEvaluator expressionEvaluator() {
        return new ExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataExpressionParser dataExpressionParser(ExpressionEvaluator evaluator) {
        return new DataExpressionParser(evaluator);
    }
}
