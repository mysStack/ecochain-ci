def call(Map cfg) {
    // 完整配置 - 包含所有功能
    def fullConfig = [
        projectKey: cfg.projectKey,
        src: cfg.src ?: 'src/main/java',
        enableTest: true,
        enableScan: true,
        enableDepScan: true,
        enableArchive: true,
        testCmd: 'mvn clean test',
        buildCmd: 'mvn clean package -DskipTests -Dbuild.version=${env.BUILD_VERSION}',
        mvnOpts: '-Xmx1024m',
        artifacts: '**/target/*.jar,**/target/*.war',
        depScanSeverity: 'HIGH,CRITICAL',
        depScanSkipDirs: ['target', '.git', '.idea'],
        notifyOnSuccess: true,
        notifyOnFailure: true,
        buildVersion: cfg.buildVersion ?: "v${new Date().format('yyyyMMdd-HHmmss')}",
        env: cfg.env ?: 'dev',
        notifyEmail: cfg.notifyEmail ?: '',
        timeout: cfg.timeout ?: 120,
        parallel: cfg.parallel ?: false,
        dockerImage: cfg.dockerImage ?: 'maven:3.9-eclipse-temurin-17',
        projectName: cfg.projectName ?: cfg.projectKey.split(':')[1],
        projectVersion: cfg.projectVersion ?: '1.0.0',
        binaries: cfg.binaries ?: 'target/classes',
        host: cfg.host ?: '',
        login: cfg.login ?: '',
        exclusions: cfg.exclusions ?: '**/test/**,**/generated/**',
        coverage: cfg.coverage ?: 'target/site/jacoco/jacoco.xml',
        sourceEncoding: cfg.sourceEncoding ?: 'UTF-8',
        javaVersion: cfg.javaVersion ?: '11',
        extraParams: cfg.extraParams ?: []
    ]

    // 合并用户提供的配置
    def finalConfig = fullConfig + cfg

    // 调用主方法
    javaMavenCI(finalConfig)
}
