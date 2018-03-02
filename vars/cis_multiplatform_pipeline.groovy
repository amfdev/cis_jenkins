
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

def executeNode(String taskType, String taskName, String nodeTags, def executeFunction, Map options)
{
    node(nodeTags) {
        stage(taskName) {
            ws("WS/${options.projectName}_${taskType}") {
                withEnv(["CIS_LOG=${WORKSPACE}/${taskName}.log"]) {
                    try {
                        if(options.get("${taskType}.cleandir", false) == true) {
                            deleteDir()
                        }

                        logEnvironmentInfo()
                        executeFunction()
                    }
                    catch (e) {
                        currentBuild.result = "${taskType} failed"
                        throw e
                    }
                    finally {
                        stash includes: "${taskName}.log", name: "log${taskName}"
                    }
                }
            }
        }
    }
}

def readOption(Map options, String key)
{
    return options.get(key)
}
def readOption(Map options, String key, def defaultValue)
{
    return options.get(key, defaultValue)
}
def executeBuild(String target, Map options)
{
    String taskType = "build"
    String taskName = "${taskType}-${target}"
    List nodeTags = [] << readOption(options, "${taskType}.tag") 
    nodeTags << readOption(options, "build.platform.tag.${target}", target)
    
    def executeFunction = readOption(options, "${taskType}.function.${target}")
    if(!executeFunction)
        executeFunction = readOption(options, "${taskType}.function")
    if(!executeFunction)
    {
        error "${taskType}.function is not defined for target ${target}"
    }
    executeNode(taskType, taskName, nodeTags.join(" && "), { executeFunction(target, options) }, options)
}

def testTask(String target, String profile, Map options)
{
    String taskType = "test"
    String taskName = "${taskType}-${target}-${profile}"
    List nodeTags = [] << readOption(options, "${taskType}.tag") 
    nodeTags << readOption(options, "test.platform.tag.${target}", target)
    nodeTags << profile
    
    def executeFunction = readOption(options, "${taskType}.function.${target}")
    if(!executeFunction)
        executeFunction = readOption(options, "${taskType}.function")
    if(!executeFunction)
    {
        error "${taskType}.function is not defined for target ${target}"
    }

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

            if(profileList && profileList.size())
            {
                def tasks = [:]
                profileList.each()
                {
                    String profile = it
                    def taskName, taskBody = testTask(target, it, options)
                    testTasks[taskName] = taskBody
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

    if(!executeFunction)
        executeFunction = readOption(options, "${taskType}.function")
    if(!executeFunction)
        return

    executeNode(taskType, taskName, "Deploy", { executeFunction(configMap, options) }, options)
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

        echo "enableNotifications = ${options.enableNotifications}"
        if("${options.enableNotifications}" == "true")
        {
            /*
            sendBuildStatusNotification(currentBuild.result, 
                                        options.get('slackChannel', ''), 
                                        options.get('slackBaseUrl', ''),
                                        options.get('slackTocken', ''))
                                        */
        }
    }
}
