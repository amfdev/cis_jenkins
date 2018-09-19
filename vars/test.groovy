
def executeBuild(String target, Map options)
{
    echo "-------------------------------------executeBuild ${target}-------------------------------------"
	dir(options['projectName'])
	{
		String testString = "/usr/bin/env bash" + System.getProperty("line.separator")
		testString += "echo \"hello\""
		writeFile("hello.sh", testString)
		stash includes: '*.sh', name: "deb-main"
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
        "usr/bin/":'deb-main'
    ]
	List deps = ['amf(1.0.0)']
	
	make_deb("hello", files, deps) 
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

        'test.tag':'test',
        'test.cleandir':true,
        'test.platform.tag.mingw_gcc_x64':'Windows',
        'test.platform.tag.mingw_gcc_x86':'Windows',
        
        'deploy.tag':'test',
		'deploy.platform.tag.mingw_gcc_x64':'Windows',
		'deploy.platform.tag.mingw_gcc_x86':'Windows',
        'deploy.cleandir':true
    ]
    
    userOptions.each()
    {
        options[it.key]=it.value
    }
    
    cis_multiplatform_pipeline(options)
}