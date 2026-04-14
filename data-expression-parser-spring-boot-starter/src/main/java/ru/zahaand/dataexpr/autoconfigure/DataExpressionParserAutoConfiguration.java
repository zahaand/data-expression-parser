package ru.zahaand.dataexpr.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import ru.zahaand.dataexpr.evaluator.ExpressionEvaluator;
import ru.zahaand.dataexpr.function.CustomFunctionRegistry;
import ru.zahaand.dataexpr.parser.DataExpressionParser;

/**
 * Spring Boot autoconfiguration for {@code data-expression-parser}.
 *
 * <p>Registers the following beans when the starter is on the classpath,
 * unless the consumer provides their own:
 * <ul>
 *   <li>{@link CustomFunctionRegistry} — empty by default; override with a consumer {@code @Bean}
 *       to register custom functions.</li>
 *   <li>{@link ExpressionEvaluator} — wired with the registry.</li>
 *   <li>{@link DataExpressionParser} — wired with evaluator and registry; injectable as a singleton.</li>
 * </ul>
 *
 * <p>All beans use {@code @ConditionalOnMissingBean} so consumers can replace any of them.
 *
 * <p>To add custom functions, define a {@code CustomFunctionRegistry} bean:
 * <pre>{@code
 * @Bean
 * public CustomFunctionRegistry customFunctionRegistry() {
 *     return CustomFunctionRegistry.builder()
 *         .register("TAX", (args, ctx) -> args[0] * 0.15)
 *         .build();
 * }
 * }</pre>
 */
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
