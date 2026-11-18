package com.payroll.architecture;

import com.payroll.entity.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Architecture & Cross-Domain SQL Join Boundary Tests for the Payroll Modular Monolith.
 *
 * <p>Enforces the boundary rules specified in docs/02-system-analysis/05-System-Design-and-Architecture.md:
 * modules must NOT perform direct SQL/JPQL JOINs or JPA entity relationship joins across
 * different domain schemas/bounded contexts.</p>
 */
class ModularMonolithBoundaryTest {

    enum BoundedContext {
        ORGANIZATION,
        REGULATORY,
        EMPLOYEE_COMPENSATION,
        TIME_AND_INPUTS,
        PAYROLL_EXECUTION,
        ACCOUNTING_SETTLEMENT
    }

    private static final Map<String, BoundedContext> TABLE_TO_DOMAIN = Map.ofEntries(
            // ORGANIZATION
            Map.entry("companies", BoundedContext.ORGANIZATION),
            Map.entry("branches", BoundedContext.ORGANIZATION),
            Map.entry("departments", BoundedContext.ORGANIZATION),
            Map.entry("positions", BoundedContext.ORGANIZATION),
            Map.entry("cost_centers", BoundedContext.ORGANIZATION),
            Map.entry("work_locations", BoundedContext.ORGANIZATION),
            Map.entry("company_policy_versions", BoundedContext.ORGANIZATION),

            // REGULATORY
            Map.entry("rule_set_versions", BoundedContext.REGULATORY),
            Map.entry("tax_ter_brackets", BoundedContext.REGULATORY),
            Map.entry("tax_pasal17_brackets", BoundedContext.REGULATORY),
            Map.entry("bpjs_program_configs", BoundedContext.REGULATORY),
            Map.entry("overtime_multiplier_rules", BoundedContext.REGULATORY),

            // EMPLOYEE_COMPENSATION
            Map.entry("employees", BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry("employments", BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry("salary_components", BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry("employee_salary_assignments", BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry("employee_ytd_opening_balances", BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry("employee_loans", BoundedContext.EMPLOYEE_COMPENSATION),

            // TIME_AND_INPUTS
            Map.entry("attendance_summaries", BoundedContext.TIME_AND_INPUTS),
            Map.entry("payroll_variable_inputs", BoundedContext.TIME_AND_INPUTS),
            Map.entry("simulator_overrides", BoundedContext.TIME_AND_INPUTS),

            // PAYROLL_EXECUTION
            Map.entry("payroll_periods", BoundedContext.PAYROLL_EXECUTION),
            Map.entry("payroll_runs", BoundedContext.PAYROLL_EXECUTION),
            Map.entry("payroll_results", BoundedContext.PAYROLL_EXECUTION),
            Map.entry("payroll_details", BoundedContext.PAYROLL_EXECUTION),

            // ACCOUNTING_SETTLEMENT
            Map.entry("journals", BoundedContext.ACCOUNTING_SETTLEMENT),
            Map.entry("journal_lines", BoundedContext.ACCOUNTING_SETTLEMENT)
    );

    private static final Map<String, BoundedContext> ENTITY_SIMPLE_NAME_TO_DOMAIN = Map.ofEntries(
            Map.entry(Company.class.getSimpleName(), BoundedContext.ORGANIZATION),
            Map.entry(CompanyPolicyVersion.class.getSimpleName(), BoundedContext.ORGANIZATION),
            Map.entry(WorkLocation.class.getSimpleName(), BoundedContext.ORGANIZATION),
            Map.entry(CostCenter.class.getSimpleName(), BoundedContext.ORGANIZATION),

            Map.entry(RuleSetVersion.class.getSimpleName(), BoundedContext.REGULATORY),
            Map.entry(TaxTerBracket.class.getSimpleName(), BoundedContext.REGULATORY),
            Map.entry(TaxPasal17Bracket.class.getSimpleName(), BoundedContext.REGULATORY),
            Map.entry(BpjsProgramConfig.class.getSimpleName(), BoundedContext.REGULATORY),
            Map.entry(OvertimeMultiplierRule.class.getSimpleName(), BoundedContext.REGULATORY),

            Map.entry(Employee.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry(Employment.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry(SalaryComponent.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry(EmployeeSalaryAssignment.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry(EmployeeYtdOpeningBalance.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),
            Map.entry(EmployeeLoan.class.getSimpleName(), BoundedContext.EMPLOYEE_COMPENSATION),

            Map.entry(AttendanceSummary.class.getSimpleName(), BoundedContext.TIME_AND_INPUTS),
            Map.entry(PayrollVariableInput.class.getSimpleName(), BoundedContext.TIME_AND_INPUTS),
            Map.entry(SimulatorOverride.class.getSimpleName(), BoundedContext.TIME_AND_INPUTS),

            Map.entry(PayrollPeriod.class.getSimpleName(), BoundedContext.PAYROLL_EXECUTION),
            Map.entry(PayrollRun.class.getSimpleName(), BoundedContext.PAYROLL_EXECUTION),
            Map.entry(PayrollResult.class.getSimpleName(), BoundedContext.PAYROLL_EXECUTION),
            Map.entry(PayrollDetail.class.getSimpleName(), BoundedContext.PAYROLL_EXECUTION),

            Map.entry(Journal.class.getSimpleName(), BoundedContext.ACCOUNTING_SETTLEMENT),
            Map.entry(JournalLine.class.getSimpleName(), BoundedContext.ACCOUNTING_SETTLEMENT)
    );

    private static final List<Class<? extends Annotation>> JPA_JOIN_ANNOTATIONS = List.of(
            ManyToOne.class, OneToMany.class, OneToOne.class, ManyToMany.class, JoinColumn.class, JoinTable.class
    );

    private static final Pattern SQL_SELECT_WITH_JOIN = Pattern.compile(
            "(?is)\\bSELECT\\b.+?\\bFROM\\b.+?(\\bJOIN\\b|,)"
    );
    private static final Pattern TABLE_OR_ENTITY_REF = Pattern.compile(
            "(?is)\\b(?:FROM|JOIN)\\s+([a-zA-Z0-9_.]+)"
    );
    private static final Pattern COMMA_FROM_CLAUSE = Pattern.compile(
            "(?is)\\bFROM\\s+([a-zA-Z0-9_.,\\s]+?)(?=\\bWHERE\\b|\\bJOIN\\b|\\bINNER\\b|\\bLEFT\\b|\\bRIGHT\\b|\\bFULL\\b|\\bCROSS\\b|\\bGROUP\\b|\\bORDER\\b|\\bLIMIT\\b|;|$)"
    );

    @Test
    void everyEntityMustBeRegisteredInBoundedContextDomain() {
        List<Class<?>> entities = scanEntities();
        assertFalse(entities.isEmpty(), "Harus ada minimal 1 class @Entity di com.payroll.entity");

        List<String> unmapped = new ArrayList<>();
        for (Class<?> entity : entities) {
            Table tableAnn = entity.getAnnotation(Table.class);
            String tableName = tableAnn != null ? tableAnn.name().toLowerCase(Locale.ROOT) : entity.getSimpleName().toLowerCase(Locale.ROOT);
            boolean hasTableDomain = TABLE_TO_DOMAIN.containsKey(tableName);
            boolean hasEntityDomain = ENTITY_SIMPLE_NAME_TO_DOMAIN.containsKey(entity.getSimpleName());
            if (!hasTableDomain || !hasEntityDomain) {
                unmapped.add(entity.getSimpleName() + " (table=" + tableName + ")");
            }
        }

        assertTrue(unmapped.isEmpty(),
                "Setiap @Entity wajib terdaftar pada Bounded Context domain. Belum terdaftar: " + unmapped);
    }

    @Test
    void entitiesMustNotUseCrossDomainJpaJoinAnnotations() {
        List<Class<?>> entities = scanEntities();
        List<String> violations = new ArrayList<>();

        for (Class<?> entity : entities) {
            BoundedContext sourceDomain = ENTITY_SIMPLE_NAME_TO_DOMAIN.get(entity.getSimpleName());
            for (Field field : entity.getDeclaredFields()) {
                for (Class<? extends Annotation> ann : JPA_JOIN_ANNOTATIONS) {
                    if (field.isAnnotationPresent(ann)) {
                        Class<?> targetType = resolveTargetEntityType(field.getGenericType());
                        BoundedContext targetDomain = targetType != null
                                ? ENTITY_SIMPLE_NAME_TO_DOMAIN.get(targetType.getSimpleName())
                                : null;
                        if (targetDomain == null || targetDomain != sourceDomain) {
                            violations.add(entity.getSimpleName() + "." + field.getName()
                                    + " memakai @" + ann.getSimpleName()
                                    + " lintas domain (" + sourceDomain + " -> " + targetDomain + ")");
                        }
                    }
                }
            }
            for (Method method : entity.getDeclaredMethods()) {
                for (Class<? extends Annotation> ann : JPA_JOIN_ANNOTATIONS) {
                    if (method.isAnnotationPresent(ann)) {
                        Class<?> targetType = resolveTargetEntityType(method.getGenericReturnType());
                        BoundedContext targetDomain = targetType != null
                                ? ENTITY_SIMPLE_NAME_TO_DOMAIN.get(targetType.getSimpleName())
                                : null;
                        if (targetDomain == null || targetDomain != sourceDomain) {
                            violations.add(entity.getSimpleName() + "." + method.getName() + "()"
                                    + " memakai @" + ann.getSimpleName()
                                    + " lintas domain (" + sourceDomain + " -> " + targetDomain + ")");
                        }
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Pelanggaran relasi JPA lintas domain ditemukan (gunakan referensi UUID): " + violations);
    }

    @Test
    void repositoriesMustNotContainCrossDomainJoinQueries() {
        List<Class<?>> repositories = scanRepositories();
        assertFalse(repositories.isEmpty(), "Harus ada minimal 1 repository di com.payroll.repository");

        List<String> violations = new ArrayList<>();
        for (Class<?> repo : repositories) {
            for (Method method : repo.getDeclaredMethods()) {
                Query queryAnn = method.getAnnotation(Query.class);
                if (queryAnn != null) {
                    String sql = queryAnn.value();
                    Optional<String> violation = detectCrossDomainJoin(sql);
                    violation.ifPresent(msg -> violations.add(repo.getSimpleName() + "." + method.getName() + ": " + msg));
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Ditemukan query @Query dengan cross-domain JOIN pada repository: " + violations);
    }

    @Test
    void sourceCodeMustNotContainRawCrossDomainSqlJoinsAndDetectorMustCatchViolations() throws IOException {
        // 1. Self-test detector against forbidden cross-domain JOIN / CROSS JOIN & allowed intra-domain JOIN
        assertCrossDomainJoinDetected("SELECT * FROM payroll_results pr JOIN employees e ON pr.employment_id = e.id");
        assertCrossDomainJoinDetected("SELECT * FROM journals j CROSS JOIN attendance_summaries a");
        assertCrossDomainJoinDetected("SELECT p FROM PayrollResult p JOIN Employment em ON p.employmentId = em.id");
        assertTrue(
                detectCrossDomainJoin("SELECT j FROM journals j JOIN journal_lines jl ON j.id = jl.journal_id").isEmpty(),
                "JOIN di dalam domain yang sama (ACCOUNTING_SETTLEMENT) seharusnya diperbolehkan."
        );

        // 2. Scan all production Java files under src/main/java/com/payroll
        Path mainJavaRoot = Paths.get("src/main/java/com/payroll");
        assertTrue(Files.isDirectory(mainJavaRoot), "Direktori src/main/java/com/payroll harus ada.");

        List<String> sourceViolations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(mainJavaRoot)) {
            List<Path> javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String literal : extractStringLiterals(content)) {
                    if (SQL_SELECT_WITH_JOIN.matcher(literal).find()) {
                        detectCrossDomainJoin(literal).ifPresent(msg ->
                                sourceViolations.add(mainJavaRoot.relativize(file) + " -> " + msg));
                    }
                }
            }
        }

        assertTrue(sourceViolations.isEmpty(),
                "Ditemukan raw SQL/JPQL cross-domain JOIN pada source code produksi: " + sourceViolations);
    }

    @Test
    void calculatorsMustBePureAndFreeOfDatabaseDependencies() {
        List<Class<?>> calculators = scanCalculatorClasses();
        assertFalse(calculators.isEmpty(), "Harus ada minimal 1 calculator di com.payroll.service.calculator");

        List<String> violations = new ArrayList<>();
        for (Class<?> calc : calculators) {
            for (Field field : calc.getDeclaredFields()) {
                if (isDatabaseType(field.getType())) {
                    violations.add(calc.getSimpleName() + " field " + field.getName() + " (" + field.getType().getName() + ")");
                }
            }
            for (Constructor<?> ctor : calc.getDeclaredConstructors()) {
                for (Class<?> param : ctor.getParameterTypes()) {
                    if (isDatabaseType(param)) {
                        violations.add(calc.getSimpleName() + " constructor param " + param.getName());
                    }
                }
            }
            for (Method method : calc.getDeclaredMethods()) {
                if (isDatabaseType(method.getReturnType())) {
                    violations.add(calc.getSimpleName() + "." + method.getName() + " return type " + method.getReturnType().getName());
                }
                for (Class<?> param : method.getParameterTypes()) {
                    if (isDatabaseType(param)) {
                        violations.add(calc.getSimpleName() + "." + method.getName() + " param " + param.getName());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Class di service.calculator wajib pure logic tanpa dependensi database/repository: " + violations);
    }

    private void assertCrossDomainJoinDetected(String query) {
        Optional<String> result = detectCrossDomainJoin(query);
        assertTrue(result.isPresent(), "Validator gagal mendeteksi pelanggaran cross-domain join pada query: " + query);
    }

    Optional<String> detectCrossDomainJoin(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String normalized = query.replaceAll("\\s+", " ").trim();
        boolean hasExplicitJoinKeyword = normalized.toUpperCase(Locale.ROOT).contains(" JOIN ");
        Set<String> referencedTablesOrEntities = new LinkedHashSet<>();

        Matcher refMatcher = TABLE_OR_ENTITY_REF.matcher(normalized);
        while (refMatcher.find()) {
            String rawName = stripSchemaPrefix(refMatcher.group(1));
            referencedTablesOrEntities.add(rawName);
        }

        Matcher commaMatcher = COMMA_FROM_CLAUSE.matcher(normalized);
        if (commaMatcher.find()) {
            String fromSegment = commaMatcher.group(1);
            if (fromSegment.contains(",")) {
                hasExplicitJoinKeyword = true;
                for (String part : fromSegment.split(",")) {
                    String token = part.trim().split("\\s+")[0];
                    if (!token.isEmpty()) {
                        referencedTablesOrEntities.add(stripSchemaPrefix(token));
                    }
                }
            }
        }

        if (!hasExplicitJoinKeyword && referencedTablesOrEntities.size() <= 1) {
            return Optional.empty();
        }

        Map<BoundedContext, Set<String>> domainsFound = new EnumMap<>(BoundedContext.class);
        for (String name : referencedTablesOrEntities) {
            BoundedContext domain = resolveDomain(name);
            if (domain != null) {
                domainsFound.computeIfAbsent(domain, k -> new LinkedHashSet<>()).add(name);
            }
        }

        if (domainsFound.size() > 1) {
            return Optional.of("Melibatkan JOIN lintas domain " + domainsFound + " pada query: \"" + normalized + "\"");
        }
        return Optional.empty();
    }

    private String stripSchemaPrefix(String rawName) {
        int dotIdx = rawName.lastIndexOf('.');
        String cleaned = dotIdx >= 0 ? rawName.substring(dotIdx + 1) : rawName;
        return cleaned.replaceAll("[\"`]", "");
    }

    private BoundedContext resolveDomain(String tableOrEntityName) {
        BoundedContext byTable = TABLE_TO_DOMAIN.get(tableOrEntityName.toLowerCase(Locale.ROOT));
        if (byTable != null) {
            return byTable;
        }
        return ENTITY_SIMPLE_NAME_TO_DOMAIN.get(tableOrEntityName);
    }

    private List<String> extractStringLiterals(String javaSource) {
        List<String> literals = new ArrayList<>();
        Matcher textBlock = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL).matcher(javaSource);
        while (textBlock.find()) {
            literals.add(textBlock.group(1));
        }
        String withoutTextBlocks = textBlock.replaceAll(" ");
        Matcher singleLine = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"").matcher(withoutTextBlocks);
        while (singleLine.find()) {
            literals.add(singleLine.group(1));
        }
        return literals;
    }

    private boolean isDatabaseType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("com.payroll.repository")
                || EntityManager.class.isAssignableFrom(type)
                || DataSource.class.isAssignableFrom(type)
                || name.equals("org.springframework.jdbc.core.JdbcTemplate")
                || name.equals("org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")
                || org.springframework.data.repository.Repository.class.isAssignableFrom(type);
    }

    private Class<?> resolveTargetEntityType(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            for (Type arg : pt.getActualTypeArguments()) {
                if (arg instanceof Class<?> cls && cls.isAnnotationPresent(Entity.class)) {
                    return cls;
                }
            }
        } else if (genericType instanceof Class<?> cls && cls.isAnnotationPresent(Entity.class)) {
            return cls;
        }
        return null;
    }

    private List<Class<?>> scanEntities() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        return loadScannedClasses(scanner.findCandidateComponents("com.payroll.entity"));
    }

    private List<Class<?>> scanRepositories() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(JpaRepository.class));
        return loadScannedClasses(scanner.findCandidateComponents("com.payroll.repository"));
    }

    private List<Class<?>> scanCalculatorClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> !metadataReader.getClassMetadata().isAbstract());
        return loadScannedClasses(scanner.findCandidateComponents("com.payroll.service.calculator")).stream()
                .filter(c -> !c.isRecord() && !c.getName().contains("$"))
                .toList();
    }

    private List<Class<?>> loadScannedClasses(Set<BeanDefinition> defs) {
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition bd : defs) {
            try {
                classes.add(Class.forName(bd.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                fail("Gagal memuat class: " + bd.getBeanClassName(), e);
            }
        }
        return classes;
    }
}
