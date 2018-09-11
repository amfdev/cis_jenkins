
def buildHelper(String target)
{
	bat"""
		wsl ./scripts/build.sh ${target} build debug
	"""
}


def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
    
    //build project
	dir("thirdparty")
	{
		cis_checkout_scm('master', "https://github.com/amfdev/thirdparty.git")
	}
	bat '''
			git clone https://github.com/amfdev/FFmpeg_dev.git FFmpeg
		'''
    dir(options['projectName'])
    {
		dir("scripts")
		{
			bat '''
				wsl ./fetch_AMF.sh
				wsl ./fetch_FFmpeg.sh
			'''
		}
		buildHelper(target)
    }

    dir(options['projectName'] + "_build-" + target+"-debug"))
    {
        bat "echo ${target} > testout.txt"
        stash includes: '*.exe', name: "${target}-binaries"
		stash includes: '*.dll', name: "${target}-libs"
    }
	echo "-----------------------------------------end----------------------------------------------------"
}

def executeTests(String target, String profile, Map options)
{
	echo "-------------------------------------executeTests ${target}-------------------------------------"
    
    dir(target)
    {
        unstash "${target}-binaries"
		unstash "${target}-libs"
		bat "echo executeTests ${target}-${profile} >> ${CIS_LOG} 2>&1"
		bat "ffmpeg.exe -version >> ${CIS_LOG} 2>&1"
    }
	echo "-----------------------------------------end----------------------------------------------------"
}

def executeDeploy(Map configMap, Map options)
{
    echo "-------------------------------------executeDeploy-------------------------------------"
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
	echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw_gcc_x64:test',
        
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
