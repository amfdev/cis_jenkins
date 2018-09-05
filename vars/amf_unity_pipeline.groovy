def buildPluginHelper(String target)
{
	echo "build AMF"
	dir("UnityIntegrations/AmfUnityPlugin/source/AMF/amf/public/samples")
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
	dir("UnityIntegrations/AmfUnityPlugin")
	{
		bat '''
			set msbuild="C:/Program Files (x86)/Microsoft Visual Studio/2017/Community/MSBuild/15.0/Bin/msbuild.exe"
			set toolset=v140

			::build AmfUnityPlugin
			%msbuild% AmfUnityPlugin.sln /property:Configuration=Release /property:Platform=x64 /p:PlatformToolset=%toolset% /m
		'''
		bat '''
			call makeDist.cmd
		'''
	}
}

def buildSamplesHelper(String target)
{
	dir("Scripts/Samples")
	{
		bat '''
			call makeDist.cmd

			call dist2Proj.cmd /../../Samples/VideoPlayBackSample
			call build_unity_project.bat /../../Samples/VideoPlayBackSample
		'''
	}
}

def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir("Unity")
	{
		cis_checkout_scm('master', "https://github.com/amfdev/Unity_dev.git")
		dir("Scripts")
		{
			bat '''
				call fetch_all.bat
			'''
		}
		buildPluginHelper(target)
		buildSamplesHelper(target)
		dir("Bin")
		{
			echo "stash includes:  'Dist/*', name: 'dist'"
			stash includes:  'Dist/*', name: 'dist'
		}
		dir("Bin/VideoPlaybackSample/win64")
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