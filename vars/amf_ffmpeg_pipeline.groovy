
def buildHelper(String target)
{
	bat"""
		ubuntu run sh -c './build.sh ${target}' >> ${CIS_LOG} 2>&1
	"""
}


def executeBuild(String target, Map options)
{
    echo "executeBuild ${target}"
    
    //build project
    dir(options['projectName'])
    {
		cis_checkout_scm("master", "https://github.com/amfdev/FFmpeg_dev.git")
		dir("Sources")
		{
			cis_checkout_scm(options['projectBranch'], options['projectRepo'])
		}
		dir("AMF/include/AMF")
		{
			
		}
		dir("scripts/AMF")
		{
			cis_checkout_scm("master", "https://github.com/GPUOpen-LibrariesAndSDKs/AMF.git")
			bat '''
				cp -R /amf/public/include/* ../AMF/include/AMF
			'''
			deleteDir()
		}
		buildHelper(target)
		
    }

    dir("${options.projectName}_redist/${target}")
    {
        bat "echo ${target} > testout.txt"
        stash includes: '**/*', name: "app-${target}"
    }
}

def executeTests(String target, String profile, Map options)
{
    dir(target)
    {
        unstash "app-${target}"
        dir('bin')
        {
            bat "echo executeTests ${target}-${profile} >> ${CIS_LOG} 2>&1"
            bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
        }
    }
}

def executeDeploy(Map configMap, Map options)
{
    configMap.each()
    {
        dir(it.key)
        {
            unstash "app-${it.key}"
            dir('bin')
            {
                bat "echo deploy ${it.key} >> ${CIS_LOG} 2>&1"
                bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
            }
        }
    }
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw_gcc_x64,mingw_gcc_x86:gpuAMD_RXVEGA',
        
        projectGroup:'AMF',

        projectName:'FFmpeg',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/FFmpeg.git',

        projectName_AMF:'AMF',
        projectBranch_AMF:'master',
        projectRepo_AMF:'https://github.com/GPUOpen-LibrariesAndSDKs/AMF.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'BuilderAMF',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',

        'test.tag':'TesterAMF',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        
        'deploy.tag':'DeployerAMF',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}
