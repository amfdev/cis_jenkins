def buildHelper(String target)
{
    if("${target}" == "mingw_gcc_x64")
    {
        bat '''bash ./scripts/build.sh mingw_gcc_x64 rebuild debug'''
		echo "mingw_gcc_x64 rem bash ./build.sh mingw_gcc_x86 rebuild debug"
    }
	else if("${target}" == "mingw")
    {
        bat '''bash ./scripts/build.sh mingw_gcc_x64 rebuild debug'''
		echo "mingw rem bash ./build.sh mingw_gcc_x86 rebuild debug"
    }
    else
    {
        echo "???????????????????????????unknown target${target}??????????????????????????"
    }
}


def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir("thirdparty"
	{
		cis_checkout_scm('master', "https://github.com/amfdev/thirdparty.git")
	}
	dir("HandBrake")
	{
		cis_checkout_scm('master', "https://github.com/amfdev/HandBrake_dev")
		dir("Sources")
		{
			cis_checkout_scm('master', "https://github.com/amfdev/HandBrake.git")
		}
		buildHelper(target)
    }
    echo "-----------------------------------------end----------------------------------------------------"
}

def executeTests(String target, String profile, Map options)
{
	echo "-------------------------------------executeTests ${target}-------------------------------------"
    
    echo "-----------------------------------------end----------------------------------------------------"
}

def executeDeploy(Map configMap, Map options)
{
	echo "-------------------------------------executeDeploy ${target}-------------------------------------"
    
    echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw_gcc_x64:test',
        
        projectGroup:'AMF',

        projectName:'HB',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/HandBrake_dev.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'test',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',
        'build.platform.tag.mingw_msvc_x64':'mingw',
        'build.platform.tag.mingw_msvc_x86':'mingw',

        'test.tag':'test',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        'test.platform.tag.mingw_msvc_x64':'Windows',
        'test.platform.tag.mingw_msvc_x86':'Windows',
        
        'deploy.tag':'test',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}