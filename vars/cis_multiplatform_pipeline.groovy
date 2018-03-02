
def logEnvironmentInfo()
{
    if(isUnix())
    {
         sh "uname -a   >  ${CIS_LOG}.log"
         sh "env        >> ${CIS_LOG}.log"
    }
    else
    {
         bat "HOSTNAME  >  ${CIS_LOG}.log"
         bat "set       >> ${CIS_LOG}.log"
    }
}

def executeBuild(String target, Map options)
{
    String taskType = "build"
    String taskName = "${taskType}-${target}"
    String taskTag = options.get("${taskType}.tag", "${taskType}")
    List nodeTags = [] << taskTag << target
    
    def executeFunction = options.get('${taskType}.function.${target}', null)
    if(!executeFunction)
        executeFunction = options.get('${taskType}.function', null)
    if(!executeFunction)
        throw new Exception("${taskType}.function is not defined for target ${target}")
    
    node(nodeTags.join(" && ")) {
        stage(taskName) {
            ws("WS/${options.PRJ_NAME}_${taskType}") {
                withEnv("CIS_LOG=${WORKSPACE}/${taskName}.log") {
                    try {
                        if(options.get("${taskType}.cleandir", false) == true) {
                            deleteDir()
                        }

                        logEnvironmentInfo()
                        executeFunction(target, options)
                    }
                    catch (e) {
                        currentBuild.result = "${taskType} failed"
                        throw e
                    }
                    finally {
                        stash "${LOG_PATH}.log" "log${taskName}"
                    }
                }
            }
        }
    }
}

def testTask(String target, String profile, Map options)
{
    String taskType = "Test"
    String taskName = "${taskType}-${target}-${profile}"
    def ret = {
        node("${target} && ${profile} && ${options.TESTER_TAG}") {
            stage(taskName)
            {
                ws("WS/${options.PRJ_NAME}_Test")
                {
                    withEnv("CIS_LOG=${WORKSPACE}/${taskName}.log") {
                        try
                        {
                            if(options['CLEAN_TEST_DIR'] == true)
                                deleteDir()

                            logEnvironmentInfo()

                            def executeFunction = options.get('${target}', null)
                            if(!executeFunction)
                                throw new Exception("executeBuild is not defined for target ${target}")
                            executeFunction(target, options)
                        }
                        catch (e) {
                            currentBuild.result = "BUILD FAILED"
                            throw e
                        }
                        finally {
                            //archiveArtifacts "${STAGE_NAME}.log"
                        }
                    }
                }
            }
        }
    }
}

def platformTask(String target, List profileList, Map options)
{
    def retNode =  
    {
        try {
            executeBuild(target, options)
            
            if(profileList.size())
            {
                def tasks = [:]
                profileList.each()
                {
                    String profile = it
                    taksName, taskBody = testTask(target, it, options)
                    testTasks[taksName] = taskBody
                }
                parallel tasks
            }
        }
        catch (e) {
            println(e.getMessage());
            currentBuild.result = "TEST FAILED"
        }
    }
    return retNode
}

def executeDeploy(Map configMap, Map options)
{
    def deployFunction = options.get('${deploy.function}', null)
    if(!deployFunction)
        return

    node("${options.DEPLOYER_TAG}")
    {
        stage("Deploy")
        {
            ws("WS/${options.PRJ_NAME}_Deploy")
            {
                try
                {
                    if(options['CLEAN_DEPLOY_DIR'] == true)
                    {
                        deleteDir()
                    }
                    deployFunction(configMap, options)
                }
                catch (e) {
                    println(e.getMessage());
                    currentBuild.result = "DEPLOY FAILED"
                }
            }
        }
    }
}

def call(String configString, Map options) {
    
    try {
        
        properties([[$class: 'BuildDiscarderProperty', strategy: 
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                      artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
        
        timestamps {
            String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
            String REF_PATH="${PRJ_PATH}/ReferenceImages"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['REF_PATH']="${REF_PATH}"
            options['JOB_PATH']="${JOB_PATH}"
            
            if(options.get('BUILDER_TAG', '') == '')
                options['BUILDER_TAG'] = 'Builder'

            if(options.get('DEPLOYER_TAG', '') == '')
                options['DEPLOYER_TAG'] = 'Deploy'

            if(options.get('TESTER_TAG', '') == '')
                options['TESTER_TAG'] = 'Tester'

            if(options.get('CLEAN_BUILD_DIR', '') == '')
                options['CLEAN_BUILD_DIR'] = 'false'

            if(options.get('CLEAN_TEST_DIR', '') == '')
                options['CLEAN_TEST_DIR'] = 'false'
            
            if(options.get('CLEAN_DEPLOY_DIR', '') == '')
                options['CLEAN_DEPLOY_DIR'] = 'true'
            
            def configMap = [:];

            configString.split(';').each()
            {
                List tokens = it.tokenize(':')
                String target = tokens.get(0)
                String profiles = tokens.get(1)
                configMap[target] = []
            
                profiles.split(',').each()
                {
                    String profile = it
                    configMap[target] << profile
                }
            }
            
            try {
                def tasks = [:]

                configMap.each()
                {
                    tasks[it.key]=platformTask(it.key, it.value, options)
                }
                parallel tasks
                
                executeDeploy()
            }
            finally
            {
            }
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());
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
