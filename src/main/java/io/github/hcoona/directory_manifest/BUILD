package(
    default_visibility = ["//src:__subpackages__"],
)

java_binary(
    name = "directory_manifest",
    srcs = [
        "App.java"
    ],
    main_class = "io.github.hcoona.directory_manifest.App",
    deps = [
        ":directory_manifest_controller",
        "@org_slf4j_slf4j_api//jar"
    ],
    runtime_deps = [
        "@ch_qos_logback_logback_classic//jar",
        "@ch_qos_logback_logback_core//jar"
    ]
)

java_library(
    name = "directory_manifest_controller",
    srcs = [
        "Controller.java",
        "ManifestFileVisitor.java"
    ],
    deps = [
        ":manual_reset_event",
        ":log_error_scheduled_thread_pool_executor",
        "@org_slf4j_slf4j_api//jar",
        "@com_google_guava_guava//jar",
        "@commons_codec_commons_codec//jar",
    ]
)

java_library(
    name = "manual_reset_event",
    srcs = [
        "ManualResetEvent.java"
    ]
)

java_library(
    name = "log_error_scheduled_thread_pool_executor",
    srcs = [
        "LogErrorScheduledThreadPoolExecutor.java"
    ],
    deps = [
        "@org_slf4j_slf4j_api//jar",
    ]
)
