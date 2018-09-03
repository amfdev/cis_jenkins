def buildHelper(String target)
{
    if("${target}" == "mingw" || "${target}" == "mingw_gcc_x64")
    {
        bat '''bash ./scripts/build.sh mingw_gcc_x64 rebuild debug'''
		echo "mingw_gcc_x64 rem bash ./build.sh mingw_gcc_x86 rebuild debug"
    }
    else
    {
        echo "???????????????????????????unknown target${target}??????????????????????????"
    }
}

def buildGuiHelper(String target)
{
	if("${target}" == "mingw" || "${target}" == "mingw_gcc_x64")
    {
		bat '''
			set msbuild="C:/Program Files (x86)/Microsoft Visual Studio/2017/Community/MSBuild/15.0/Bin/msbuild.exe"
			set nuget="C:/Program Files (x86)/Microsoft Visual Studio/2017/Community/MSBuild/15.0/Bin/nuget.exe"
			set pathToNSIS=C:/Program Files (x86)/NSIS;
			::set toolset=v140
			set project=build.xml
			::HandBrake.sln
			set PATH=%PATH%;%pathToNSIS%
			set curDir=%cd%

			cd ../Sources/win/CS
			::%msbuild% /t:restore packages.config
			%nuget% install packages.config
			%nuget% restore
			%msbuild% %project% /property:Configuration=Release /t:Release /property:Platform=x64 /p:PlatformToolset=%toolset% /m

			copy %curDir%/../_build-mingw_gcc_x64-debug/libhb/hb.dll %curDir%/../Sources/win/CS/HandBrakeWPF/bin/x64/Release/hb.dll
			cd %curDir%
		'''
    }
    else
    {
        echo "???????????????????????????unknown target${target}??????????????????????????"
    }
}

def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir("thirdparty")
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
		buildGuiHelper(target)
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