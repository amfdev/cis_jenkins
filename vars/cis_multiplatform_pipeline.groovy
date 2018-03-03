
def logEnvironmentInfo()
{
    if(isUnix())
    {
         sh "uname -a   >  ${CIS_LOG}"
         sh "env        >> ${CIS_LOG}"
    }
    else
    {
         bat "HOSTNAME  >  ${CIS_LOG}"
         bat "set       >> ${CIS_LOG}"
    }
}

def str(String str)
{
    return str
}

def executeNode(String taskType, String taskName, String nodeTags, def executeFunction, Map options)
{
    node(nodeTags) {
        stage(taskName) {
            ws("WS/${options.projectName}_${taskType}") {
                withEnv(["CIS_LOG=${WORKSPACE}/_sys/${taskName}.log"]) {
                    try {
                        dir('_sys')
                        {
                            deleteDir()
                            logEnvironmentInfo()
                        }
                        
                        dir('ws')
                        {
                            if(options.get(str("${taskType}.cleandir"), false) == true) {
                                deleteDir()
                            }
                            executeFunction()
                        }
                    }
                    catch (e) {
                        currentBuild.result = "${taskType} failed"
                        throw e
                    }
                    finally {
                        dir('_sys')
                        {
                            stash includes: "${taskName}.log", name: "log${taskName}"
                        }
                        archiveArtifacts "_sys/${taskName}.log"
                    }
                }
            }
        }
    }
}

def executeBuild(String target, Map options)
{
    String taskType = "build"
    String taskName = "${taskType}-${target}"
    List nodeTags = [] << options.get(str("${taskType}.tag")) 
    nodeTags << options.get(str("build.platform.tag.${target}"), target)
    
    def executeFunction = options.get(str("${taskType}.function.${target}"))
    if(!executeFunction)
        executeFunction = options.get(str("${taskType}.function"))
    if(!executeFunction)
    {
        error "${taskType}.function is not defined for target ${target}"
    }
    executeNode(taskType, taskName, nodeTags.join(" && "), { executeFunction(target, options) }, options)
}

def testTask(String target, String profile, Map options)
{
    echo "0"
    String taskType = "test"
    String taskName = "${taskType}-${target}-${profile}"
    List nodeTags = [] << options.get(str("${taskType}.tag")) 
    nodeTags << options.get(str("test.platform.tag.${target}"), target)
    nodeTags << profile
    echo "1"

    def executeFunction = options.get(str("${taskType}.function.${target}"))
    if(!executeFunction)
        executeFunction = options.get(str("${taskType}.function"))
    if(!executeFunction)
    {
        error "${taskType}.function is not defined for target ${target}"
    }
    echo "2"

    def ret = {
        executeNode(taskType, taskName, nodeTags.join(" && "), { executeFunction(target, profile, options) }, options)
    }
    return ret
}

def platformTask(String target, List profileList, Map options)
{
    def retNode =  
    {
       try {
            executeBuild(target, options)

            echo "parsing tests"
            if(profileList && profileList.size())
            {
                def tasks = [:]
                profileList.each()
                {
                    String profile = it
                    echo "${profile}"
                    def taskName, taskBody = testTask(target, profile, options)
                    echo "${taskName}"
                    tasks[taskName] = taskBody
                }
                parallel tasks
            }
        }
        catch (e) {
            println(e.toString());
            println(e.getMessage());
            throw e
        }
    }
    return retNode
}

def executeDeploy(Map configMap, Map options)
{
    String taskType = "deploy"
    String taskName = "deploy"
    String nodeName = options.get(str("${taskType}.tag"))
    
    def executeFunction = options.get(str("${taskType}.function"))
    if(!executeFunction)
        return

    executeNode(taskType, taskName, nodeName, { executeFunction(configMap, options) }, options)
}

def call(String configString, Map options) {
    
    try {
        
        properties([[$class: 'BuildDiscarderProperty', strategy: 
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                      artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
        
        timestamps {
            String PRJ_PATH="${options.projectGroup}/${options.projectName}"
            String REF_PATH="${PRJ_PATH}/ReferenceImages"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['REF_PATH']="${REF_PATH}"
            options['JOB_PATH']="${JOB_PATH}"
            
            
            def configMap = [:];

            configString.split(';').each()
            {
                List tokens = it.tokenize(':')
                String targets = tokens.get(0)

                List profileList;
                if(tokens.size() > 1)
                    profileList = tokens.get(1).split(',')
               
                targets.split(',').each()
                {
                    configMap[it] = profileList
                }
            }
            
            try {
                def tasks = [:]

                configMap.each()
                {
                    tasks[it.key]=platformTask(it.key, it.value, options)
                }
                parallel tasks
            }
            finally
            {
                executeDeploy(configMap, options)
            }
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {

    }
}
