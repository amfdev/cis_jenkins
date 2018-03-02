
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

def executePlatform(String target, List testProfileList, Map options)
{
    def retNode =  
    {
        try {
            node("${target} && ${options.BUILDER_TAG}") {
                
                echo "Scheduling Build ${osName}"

                stage("Build-${osName}") {
                    ws("WS/${options.PRJ_NAME}_Build") {
                        withEnv("CIS_LOG=${WORKSPACE}/${STAGE_NAME}.log") {
                            try {
                                if(options['BUILD_CLEAN'] == true) {
                                    deleteDir()
                                }
                                
                                logEnvironmentInfo()
                                
                                def executeBuild = options.get('${target}', null)
                                if(!executeBuild)
                                    throw new Exception("executeBuild is not defined for target ${target}")
                                executeBuild(target, options)
                            }
                            catch (e) {
                                currentBuild.result = "BUILD FAILED"
                                throw e
                            }
                            finally {
                                stash "${LOG_PATH}.log" "log-Build-${osName}"
                            }
                        }
                    }
                }
            }

            if(gpuNames)
            {
                def testTasks = [:]
                gpuNames.split(',').each()
                {
                    String asicName = it
                    echo "Scheduling Test ${osName}:${asicName}"

                    testTasks["Test-${it}-${osName}"] = {
                        node("${osName} && ${options.TESTER_TAG} && gpu${asicName}")
                        {
                            stage("Test-${asicName}-${osName}")
                            {
                                ws("WS/${options.PRJ_NAME}_Test")
                                {
                                    try
                                    {
                                        if(options['cleanDirs'] == true)
                                        {
                                            echo 'cleaning directory'
                                            deleteDir()
                                        }

                                        Map newOptions = options.clone()
                                        newOptions['testResultsName'] = "testResult-${asicName}-${osName}"
                                        executeTests(osName, asicName, newOptions)
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
                parallel testTasks
            }
            else
            {
                echo "No tests found for ${osName}"
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
    def deployFunction = options.get('${deployFunction}', null)
    if(!deployFunction)
        throw new Exception("deployFunction is not defined")

    node("${options.DEPLOYER_TAG}")
    {
        stage("Deploy")
        {
            ws("WS/${options.PRJ_NAME}_Deploy")
            {
                try
                {
                    if(options['cleanDirs'] == true)
                    {
                        echo 'cleaning directory'
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

            if(options.get('BUILD_CLEAN', '') == '')
                options['BUILD_CLEAN'] = 'false'

            if(options.get('TEST_CLEAN', '') == '')
                options['TEST_CLEAN'] = 'false'
            
            if(options.get('DEPLOY_CLEAN', '') == '')
                options['DEPLOY_CLEAN'] = 'true'
            
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
                    tasks[it.key]=executePlatform(it.key, it.value, options)
                }
                parallel tasks
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
