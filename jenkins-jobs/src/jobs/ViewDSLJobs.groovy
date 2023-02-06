listView('Jenkins Tutorial Demo - Part III - DSL') {
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
