load("@bazel_tools//tools/build_defs/repo:maven_rules.bzl",
    "maven_jar", "maven_dependency_plugin")

maven_server(
    name = "default"
)

maven_jar(
    name = "org_slf4j_slf4j_api",
    artifact = "org.slf4j:slf4j-api:1.7.25"
)

maven_jar(
    name = "ch_qos_logback_logback_classic",
    artifact = "ch.qos.logback:logback-classic:1.2.3"
)

maven_jar(
    name = "ch_qos_logback_logback_core",
    artifact = "ch.qos.logback:logback-core:1.2.3"
)
