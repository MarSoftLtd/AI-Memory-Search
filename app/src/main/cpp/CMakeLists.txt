cmake_minimum_required(VERSION 3.22.1)

project(e5_sentencepiece_jni)

set(SENTENCEPIECE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/sentencepiece)
set(SENTENCEPIECE_SRC_DIR ${SENTENCEPIECE_DIR}/src)
set(SENTENCEPIECE_PROTOBUF_DIR ${SENTENCEPIECE_DIR}/third_party/protobuf-lite)

set(PROTOBUF_LITE_SOURCES
        ${SENTENCEPIECE_PROTOBUF_DIR}/arena.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/arenastring.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/bytestream.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/coded_stream.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/common.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/extension_set.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/generated_enum_util.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/generated_message_table_driven_lite.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/generated_message_util.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/implicit_weak_message.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/int128.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/io_win32.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/message_lite.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/parse_context.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/repeated_field.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/status.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/statusor.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/stringpiece.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/stringprintf.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/structurally_valid.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/strutil.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/time.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/wire_format_lite.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/zero_copy_stream.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/zero_copy_stream_impl.cc
        ${SENTENCEPIECE_PROTOBUF_DIR}/zero_copy_stream_impl_lite.cc
)

set(SENTENCEPIECE_RUNTIME_SOURCES
        ${SENTENCEPIECE_SRC_DIR}/builtin_pb/sentencepiece.pb.cc
        ${SENTENCEPIECE_SRC_DIR}/builtin_pb/sentencepiece_model.pb.cc
        ${SENTENCEPIECE_SRC_DIR}/bpe_model.cc
        ${SENTENCEPIECE_SRC_DIR}/char_model.cc
        ${SENTENCEPIECE_SRC_DIR}/error.cc
        ${SENTENCEPIECE_SRC_DIR}/filesystem.cc
        ${SENTENCEPIECE_SRC_DIR}/model_factory.cc
        ${SENTENCEPIECE_SRC_DIR}/model_interface.cc
        ${SENTENCEPIECE_SRC_DIR}/normalizer.cc
        ${SENTENCEPIECE_SRC_DIR}/sentencepiece_processor.cc
        ${SENTENCEPIECE_SRC_DIR}/unigram_model.cc
        ${SENTENCEPIECE_SRC_DIR}/util.cc
        ${SENTENCEPIECE_SRC_DIR}/word_model.cc
        ${SENTENCEPIECE_DIR}/third_party/absl/flags/flag.cc
)

add_library(
        e5_sentencepiece_jni
        SHARED
        e5_sentencepiece_jni.cpp
        ${SENTENCEPIECE_RUNTIME_SOURCES}
        ${PROTOBUF_LITE_SOURCES}
)

target_include_directories(
        e5_sentencepiece_jni
        PRIVATE
        ${SENTENCEPIECE_SRC_DIR}
        ${SENTENCEPIECE_SRC_DIR}/builtin_pb
        ${SENTENCEPIECE_DIR}
        ${SENTENCEPIECE_DIR}/third_party
        ${SENTENCEPIECE_PROTOBUF_DIR}
)

target_compile_definitions(
        e5_sentencepiece_jni
        PRIVATE
        HAVE_PTHREAD=1
        _USE_INTERNAL_STRING_VIEW
)

find_library(LOG_LIB log)
target_link_libraries(e5_sentencepiece_jni PRIVATE ${LOG_LIB})

find_library(ATOMIC_LIB atomic)
if (ATOMIC_LIB)
    target_link_libraries(e5_sentencepiece_jni PRIVATE ${ATOMIC_LIB})
endif ()
