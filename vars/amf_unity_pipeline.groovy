def buildPluginHelper(String target)
{
	echo "build AMF"
	dir("Unity/UnityIntegrations/AmfUnityPlugin/source/AMF/amf/public/samples")
	{
		bat '''
			set msbuild="C:/Program Files (x86)/Microsoft Visual Studio/2017/Community/MSBuild/15.0/Bin/msbuild.exe"
			set toolset=v140
			set project=CPPSamples_vs2015.sln

			::build AMF
			%msbuild% %project% /property:Configuration=Release /property:Platform=x64 /m
		'''
	}
	echo "build AMF"
	dir("Unity/UnityIntegrations/AmfUnityPlugin")
	{
		bat '''
			set msbuild="C:/Program Files (x86)/Microsoft Visual Studio/2017/Community/MSBuild/15.0/Bin/msbuild.exe"
			set toolset=v140

			::build AmfUnityPlugin
			%msbuild% AmfUnityPlugin.sln /property:Configuration=Release /property:Platform=x64 /p:PlatformToolset=%toolset% /m
		'''
		Runtime.runtime.exec("makeDist.cmd")
	}
}

def buildSamplesHelper(String target)
{
	dir("Unity/Scripts/Samples")
	{
		Runtime.runtime.exec("build_samples.cmd")
	}
}

def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir("thirdparty")
	{
		cis_checkout_scm('master', "https://github.com/amfdev/thirdparty.git")
	}
	dir("Unity")
	{
		cis_checkout_scm('master', "https://github.com/amfdev/Unity_dev.git")
		dir("UnityIntegrations")
		{
			cis_checkout_scm('master', "https://github.com/GPUOpen-LibrariesAndSDKs/UnityIntegrations.git")
			dir("UnityIntegrations")
			{
				bat '''
					set unitytemp=unity-tmp

					where HG
					IF %ERRORLEVEL% EQU 0 (
						echo HG found
						if exist "%unitytemp%" (
							echo Directory %unitytemp% exists. Please rename or remove.
						) else (
							mkdir %unitytemp%
							pushd %unitytemp%
							echo Cloning https://bitbucket.org/Unity-Technologies/graphicsdemos
							hg clone https://bitbucket.org/Unity-Technologies/graphicsdemos
							popd
							echo Copying
							copy "%unitytemp%/graphicsdemos/NativeRenderingPlugin/PluginSource/source/Unity/*.h" source/Unity
							echo Removing %unitytemp%
							rd /s /q "%unitytemp%"
						)
					) else (
						echo Aborting, HG binary not found.
					)
				'''
			}
		}
		buildPluginHelper(target)
		buildSamplesHelper(target)
		dir("Unity/Bin")
		{
			echo "stash includes:  'Dist/*', name: 'dist'"
			stash includes:  'Dist/*', name: 'dist'
		}
		dir("Unity/Bin/VideoPlaybackSample/win64")
		{
			echo "stash includes: '*', name: 'sample'"
			stash includes: '*', name: 'sample'
		}
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
	echo "-------------------------------------executeDeploy-------------------------------------"
    dir("Dist")
	{
		unstash('dist')
	}
	dir("Sample")
	{
		unstash('sample')
	}

    echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'Unity:test',
        
        projectGroup:'AMF',

        projectName:'Unity',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/Unity_dev.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'test',

        'test.tag':'test',
        'test.cleandir':true,
        
        'deploy.tag':'test',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}