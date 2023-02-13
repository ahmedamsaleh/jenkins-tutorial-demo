job("project-dsl-seed-job-PRD") {
    label('code-k8s-agent')
    jdk('jdk-1.8.0_265')
    parameters {
        activeChoiceParam('MANIFEST_VERSION') {
            description('Manifest version')
            choiceType('SINGLE_SELECT')
            groovyScript {
                script(
                        """def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
        jenkins.model.Jenkins.instance)

def credentials = creds.findResult { it.id == 'merative-artifactory-token' ? it : null }
def password = credentials.password
def username = credentials.username

def searchUrl = "https://artifactory.commops.truvenhealth.com/artifactory/api/search/gavc?a=manifest&g=com/ibm/wh/fa/installers&repos=wh-code-stage-alpha-local,wh-code-stage-maven-local,wh-code-release-maven-local"
def conn = searchUrl.toURL().openConnection()
def base64EncodedCreds = (username + ":" + password).bytes.encodeBase64().toString()
conn.setRequestProperty("Authorization", "Basic "+ base64EncodedCreds)
def searchResults = new groovy.json.JsonSlurper().parseText(conn.content.text)

def allArtifacts = searchResults.results.collect { it.uri.tokenize("/")[-1] }
def versions = allArtifacts.findAll { (it.contains ("json")) }
        .collect { it.replaceAll(".json", "") }
        .collect { it.replaceAll("manifest-", "") }

def regexNewVersions = "\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+"
def regexOldVersions = "\\d+\\.\\d+\\.\\d+\\.\\d+"
versions = versions.findAll { (it ==~ regexNewVersions) || (it ==~ regexOldVersions) }

def versionsNew = versions.findAll { it.tokenize('.').size() > 4 }
def versionsOld = versions.findAll { it.tokenize('.').size() <= 4 }

versionsNew.sort {a, b -> a.tokenize('.')[0].toInteger() <=> b.tokenize('.')[0].toInteger()?:
        a.tokenize('.')[1].toInteger() <=> b.tokenize('.')[1].toInteger()?:
                a.tokenize('.')[2].toInteger() <=> b.tokenize('.')[2].toInteger()?:
                        a.tokenize('.')[3].toInteger() <=> b.tokenize('.')[3].toInteger()?:
                                a.tokenize('.')[4].toInteger() <=> b.tokenize('.')[4].toInteger()
}
versionsNew = versionsNew.reverse()

versionsOld.sort {a, b -> a.tokenize('.')[0].toInteger() <=> b.tokenize('.')[0].toInteger()?:
        a.tokenize('.')[1].toInteger() <=> b.tokenize('.')[1].toInteger()?:
                a.tokenize('.')[2].toInteger() <=> b.tokenize('.')[2].toInteger()?:
                        a.tokenize('.')[3].toInteger() <=> b.tokenize('.')[3].toInteger()
}
versionsOld = versionsOld.reverse()

versions = versionsNew + versionsOld
return versions"""
                )
                fallbackScript('')
            }
        }
        booleanParam('USE_JENKINS_DSL_ARCHIVE', false, "In case of any additional hotfix that can't be included to the new manifest")
        activeChoiceParam('JENKINS_DSL_ARCHIVE_VERSION') {
            description('Jenkins dsl archive version')
            choiceType('SINGLE_SELECT')
            groovyScript {
                script(
                        """def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
        jenkins.model.Jenkins.instance)

def credentials = creds.findResult { it.id == 'merative-artifactory-token' ? it : null }
def password = credentials.password
def username = credentials.username

def searchUrl = "https://artifactory.commops.truvenhealth.com/artifactory/api/search/gavc?a=jenkins-dsl&g=com/ibm/wh/fa&repos=wh-code-stage-alpha-local,wh-code-stage-maven-local,wh-code-release-maven-local"
def conn = searchUrl.toURL().openConnection()
def base64EncodedCreds = (username + ":" + password).bytes.encodeBase64().toString()
conn.setRequestProperty("Authorization", "Basic "+ base64EncodedCreds)
def searchResults = new groovy.json.JsonSlurper().parseText(conn.content.text)

def allArtifacts = searchResults.results.collect { it.uri.tokenize("/")[-1] }
def versions = allArtifacts.findAll { (it.contains ("tar")) }
        .collect { it.replaceAll(".tar", "") }
        .collect { it.replaceAll("jenkins-dsl-", "") }

def regexNewVersions = "\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+"
def regexOldVersions = "\\d+\\.\\d+\\.\\d+\\.\\d+"
versions = versions.findAll { (it ==~ regexNewVersions) || (it ==~ regexOldVersions) }

def versionsNew = versions.findAll { it.tokenize('.').size() > 4 }
def versionsOld = versions.findAll { it.tokenize('.').size() <= 4 }

versionsNew.sort {a, b -> a.tokenize('.')[0].toInteger() <=> b.tokenize('.')[0].toInteger()?:
        a.tokenize('.')[1].toInteger() <=> b.tokenize('.')[1].toInteger()?:
                a.tokenize('.')[2].toInteger() <=> b.tokenize('.')[2].toInteger()?:
                        a.tokenize('.')[3].toInteger() <=> b.tokenize('.')[3].toInteger()?:
                                a.tokenize('.')[4].toInteger() <=> b.tokenize('.')[4].toInteger()
}
versionsNew = versionsNew.reverse()

versionsOld.sort {a, b -> a.tokenize('.')[0].toInteger() <=> b.tokenize('.')[0].toInteger()?:
        a.tokenize('.')[1].toInteger() <=> b.tokenize('.')[1].toInteger()?:
                a.tokenize('.')[2].toInteger() <=> b.tokenize('.')[2].toInteger()?:
                        a.tokenize('.')[3].toInteger() <=> b.tokenize('.')[3].toInteger()
}
versionsOld = versionsOld.reverse()

versions = versionsNew + versionsOld
return versions"""
                )
                fallbackScript('')
            }
        }
    }
    wrappers {
        credentialsBinding {
            usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASS', 'merative-artifactory-token')
        }
        injectPasswords {
            injectGlobalPasswords()
        }
    }
    environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)

    }
    steps {
        systemGroovyCommand(   """
    import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

    def sa = ScriptApproval.get()
    def hashes = []

    for (def script : sa.pendingScripts) {
      println "--- Begin script ${script.hash} ---"
      println script.script
      println "--- End script ${script.hash} ---"

      hashes.add(script.hash)
    }

    for (def hash : hashes) {
      println "--- Approving script ${hash} ---"
      sa.approveScript(hash)
    }
    """)
        conditionalSteps{
            condition{
                booleanCondition('${USE_JENKINS_DSL_ARCHIVE}')
            }
            runner('DontRun')
            steps{
                buildDescription('', '${JENKINS_DSL_ARCHIVE_VERSION}')
            }
        }
        conditionalSteps {
            condition {
                not {
                    booleanCondition('${USE_JENKINS_DSL_ARCHIVE}')
                }
            }
            runner('DontRun')
            steps {
                systemGroovyCommand("""
    def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class, jenkins.model.Jenkins.instance)

    def credential = creds.findResult { it.id == 'merative-artifactory-token' ? it : null }
    def password = credential.password
    def username = credential.username
    def MANIFEST_VERSION = build.getEnvironment(listener).get('MANIFEST_VERSION')

    def manifest = ['curl', "https://artifactory.commops.truvenhealth.com/artifactory/wh-code-all-maven-virtual/com/ibm/wh/fa/installers/manifest/\${MANIFEST_VERSION}/manifest-\${MANIFEST_VERSION}.json", '-u', "\${username}:\${password}"]
    def manifestJson = new groovy.json.JsonSlurper().parseText(manifest.execute().text)

    if (manifestJson.errors) {
        println("Manifest: ${manifestJson}")
        throw new Exception("Error downloading manifest. Manifest version: ${MANIFEST_VERSION}")
    }

    def dslVersion = manifestJson.artifacts.find{it.name == "jenkins-dsl"}.version
    
    import hudson.EnvVars;
    import hudson.model.Environment;
    def build = Thread.currentThread().executable
    def vars = [JENKINS_DSL_ARCHIVE_VERSION: dslVersion]
    build.environments.add(0, Environment.create(new EnvVars(vars)))
    """)
                buildDescription('', '${MANIFEST_VERSION} => ${JENKINS_DSL_ARCHIVE_VERSION}')
            }
        }
        shell("""
            echo "Use jenkins dsl archive directly ${USE_JENKINS_DSL_ARCHIVE}."
            echo "Jenkins dsl archive version ${JENKINS_DSL_ARCHIVE_VERSION}."
            rm -rf dsl jenkins-dsl-${JENKINS_DSL_ARCHIVE_VERSION}.tar
            wget --user=${ARTIFACTORY_USER} --password=${ARTIFACTORY_PASS} https://artifactory.commops.truvenhealth.com/artifactory/wh-code-all-maven-virtual/com/ibm/wh/fa/jenkins-dsl/${JENKINS_DSL_ARCHIVE_VERSION}/jenkins-dsl-${JENKINS_DSL_ARCHIVE_VERSION}.tar
            mkdir dsl
            tar -xvf jenkins-dsl-${JENKINS_DSL_ARCHIVE_VERSION}.tar -C dsl
            cd dsl/jenkins-dsl/ && ./gradlew clean gatherStgToolchainDslFiles --no-daemon -DbranchName=master -PartifactoryUser=${ARTIFACTORY_USER} -PartifactoryPassword=${ARTIFACTORY_PASS}
        """)
        dsl {
            external('dsl/jenkins-dsl/build/dsl/*.groovy')
            ignoreExisting(false)
        }
        systemGroovyCommand("""
    import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

    def sa = ScriptApproval.get()
    def hashes = []

    for (def script : sa.pendingScripts) {
      println "--- Begin script ${script.hash} ---"
      println script.script
      println "--- End script ${script.hash} ---"

      hashes.add(script.hash)
    }

    for (def hash : hashes) {
      println "--- Approving script ${hash} ---"
      sa.approveScript(hash)
    }
    """)
    }
}