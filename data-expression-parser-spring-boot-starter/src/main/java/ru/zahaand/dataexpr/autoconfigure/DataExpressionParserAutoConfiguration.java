package ru.zahaand.dataexpr.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

@AutoConfiguration
public class DataExpressionParserAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CustomFunctionRegistry customFunctionRegistry() {
        return CustomFunctionRegistry.empty();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpressionEvaluator expressionEvaluator(CustomFunctionRegistry customFunctionRegistry) {
        return new ExpressionEvaluator(customFunctionRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataExpressionParser dataExpressionParser(ExpressionEvaluator evaluator,
                                                     CustomFunctionRegistry customFunctionRegistry) {
        return new DataExpressionParser(evaluator, customFunctionRegistry);
    }
}
