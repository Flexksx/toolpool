mod build '.just/build'
mod ci '.just/ci'
mod demo '.just/demo'
mod format '.just/format'
mod lint '.just/lint'
mod openapi '.just/openapi'
mod test '.just/test'

[private]
default:
    just --list --list-submodules
