
def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir(options['projectName'])
	{
		String testString = "/usr/bin/env bash" + System.getProperty("line.separator")
		testString += "echo \"hello\""
		String fileName = 
		writeFile file: "hello" , text: testString
		stash includes: '*', name: "deb-main"
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
	Map files = [
        "deb-main":'usr/bin/'
    ]
	List deps = ['amf (>=1.0.0)']
	
	Map info = [
        Name:'test',
		Package:'test'
		]
	
	make_deb(info, files, deps) 
    echo "-----------------------------------------end----------------------------------------------------"
}

def call(Map userOptions = [:]
        ) {

    Map options = [
        config:'mingw_gcc_x64:LinuxTest',
        
        projectGroup:'AMF',

        projectName:'Test_Deb',
        projectBranch:'master',
        projectRepo:'https://github.com/amfdev/HandBrake_dev.git',
        
        'build.function':this.&executeBuild,
        'test.function':this.&executeTests,
        'deploy.function':this.&executeDeploy,

        'build.tag':'LinuxTest',
        'build.platform.tag.mingw_gcc_x64':'mingw',
        'build.platform.tag.mingw_gcc_x86':'mingw',

        'test.tag':'LinuxTest',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'LinuxTest',
        'test.platform.tag.mingw_gcc_x86':'LinuxTest',
        
        'deploy.tag':'LinuxTest',
		'deploy.platform.tag.mingw_gcc_x64':'LinuxTest',
		'deploy.platform.tag.mingw_gcc_x86':'LinuxTest',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}