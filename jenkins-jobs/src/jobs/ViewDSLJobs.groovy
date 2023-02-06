listView('Jenkins Tutorial Demo - Part II - DSL') {
    jobs {
        regex '.+\\(DSL\\).*'
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
