def call(Map pipelineParams)
{
	pipeline
	{
		agent
  		{
    		label 'Linux'
    	}
    	options
  		{
    		buildDiscarder(
            	logRotator(
                	daysToKeepStr: '60',   // Build Records
                	artifactDaysToKeepStr: '60'  //Artifacts from builds older than this number of days will be deleted, but the logs, history, reports, etc for the build will be kept
            		)
        		)
        	timeout(50)
        	timestamps()
        	disableConcurrentBuilds()
    	}
  		environment 
		{ 
    		projectArtifactId = 'ArtifactId'
    		projectGroupId = 'GroupId'
    		projectVersion = 'Version'
     		artifactType = 'Packaging'
          	bitbucket_repo = "gal-reimbursements"
          	branch_type = 'branch_type'
          	branch = 'branch'
      	}
		stages
  		{
          	stage('Get Branch Type')
      		{
        		steps
          		{
        			sh 'git branch -r >branch1.txt'
                  	sh 'cat branch1.txt'
              		sh 'cat branch1.txt | awk -F\'/\' \'{print $2}\' >branch_type.txt'
                  	sh 'cat branch1.txt | awk -F\'/\' \'{print $3}\' >branch.txt'
                  	
              		script
              		{
              			branch_type = readFile('branch_type.txt').trim()
                  		echo "Branch Type is #${branch_type}#"
                      	branch = readFile('branch.txt').trim()
                      	echo "Branch name is #${branch}#"
                	}
            	}
       		}
    		stage("Build and Package")
      		{
        		steps
          		{
              		echo "Branch Name is : ${env.BRANCH_NAME}"
                  	echo "Branch Type is : ${branch_type}"
                  	echo "Branch name is : ${branch}"
                  	echo (pipelineParams.nexus_url)
                 	script
					{
						pom = readMavenPom file: "pom.xml"
                    	projectArtifactId = pom.getArtifactId()
						projectGroupId = pom.getGroupId()
						projectVersion = pom.getVersion()
						artifactType = pom.getPackaging()
                	}
//              		sh "export MAVEN_OPTS=-Xmx2048m"
            		sh "mvn clean install"
                  	sh '''cd templates
						jar -cvf templates.jar *.*'''
              		echo 'Build completed'
            	}
       		}
//     		stage('Sonarqube master')
//			{
//            	when
//              	{
//                	branch 'master'
//                }
//				environment
//     			{
//					SCANNER_HOME = tool 'SonarqubeScanner'
//				}
//				steps
//     			{
//                	withSonarQubeEnv('SonarQube')
//         			{
//						sh '''$SCANNER_HOME/bin/sonar-scanner \
//						-Dsonar.projectKey=REIMBURSEMENTS_MASTER \
//						-Dsonar.java.binaries=target/classes/ \
//             			-Dsonar.sources=src/main/java/'''
//					}
//                }
//            }
//          	stage('Sonarqube release')
//			{
//            	when
//              	{
//                	branch 'release/*'
//                }
//				environment
//     			{
//					SCANNER_HOME = tool 'SonarqubeScanner'
//				}
//				steps
//     			{
//                	withSonarQubeEnv('SonarQube')
//         			{
//						sh '''$SCANNER_HOME/bin/sonar-scanner \
//						-Dsonar.projectKey=REIMBURSEMENTS_RELEASE \
//						-Dsonar.java.binaries=target/classes/ \
//             			-Dsonar.sources=src/main/java/'''
//					}
//                }
//            }
//          	stage('Sonarqube development')
//			{
//            	when
//              	{
//                	branch 'develop'
//                }
//				environment
//     			{
//					SCANNER_HOME = tool 'SonarqubeScanner'
//				}
//				steps
//     			{
//                	withSonarQubeEnv('SonarQube')
//         			{
//						sh '''$SCANNER_HOME/bin/sonar-scanner \
//						-Dsonar.projectKey=REIMBURSEMENTS_DEVELOPMENT \
//						-Dsonar.java.binaries=target/classes/ \
//             			-Dsonar.sources=src/main/java/'''
//					}
//                }
//            }
//          	stage('Sonarqube feature')
//			{
//            	when
//              	{
//                	branch 'feature/*'
//                }
//				environment
//     			{
//					SCANNER_HOME = tool 'SonarqubeScanner'
//				}
//				steps
//     			{
//                	withSonarQubeEnv('SonarQube')
//         			{
//						sh '''$SCANNER_HOME/bin/sonar-scanner \
//						-Dsonar.projectKey=REIMBURSEMENTS_FEATURE \
//						-Dsonar.java.binaries=target/classes/ \
//             			-Dsonar.sources=src/main/java/'''
//					}
//                }
//            }
            stage("Uploading master WAR file to Nexus")
			{
            	when
				{
                	branch 'master'
            	}
        		steps
				{
					nexusArtifactUploader(
						artifacts: [[artifactId: "${env.BRANCH_NAME}", classifier: '', file: "target/${projectArtifactId}-${projectVersion}.${artifactType}", type: "${artifactType}"],
							[artifactId: "${env.BRANCH_NAME}",classifier: '', file: "pom.xml", type: "pom" ],
                      		[artifactId: "${env.BRANCH_NAME}",classifier: '', file: "templates/templates.jar", type: "jar" ]],
                		credentialsId: 'd9f3ff8c-9dd2-4233-856f-db2921861c1a',
                		groupId: "${bitbucket_repo}",
                		nexusUrl: (pipelineParams.nexus_url),
                		nexusVersion: 'nexus3',
                		protocol: 'http',
                		repository: (pipelineParams.nexus_prod_repo),
                      	version: "${env.BRANCH_NAME}-ims"
					)
           		}
            }
          	stage("Uploading Relese WAR file to Nexus")
			{
            	when
				{
                	branch 'release/*'
	           	}
              	steps
              	{
        			nexusArtifactUploader(
						artifacts: [[artifactId: "${branch_type}", classifier: '', file: "target/${projectArtifactId}-${projectVersion}.${artifactType}", type: "${artifactType}"],
							[artifactId: "${branch_type}",classifier: '', file: "pom.xml", type: "pom" ],
                      		[artifactId: "${branch_type}",classifier: '', file: "templates/templates.jar", type: "jar" ]],
                		credentialsId: 'd9f3ff8c-9dd2-4233-856f-db2921861c1a',
                		groupId: "${bitbucket_repo}",
                		nexusUrl: (pipelineParams.nexus_url),
                		nexusVersion: 'nexus3',
                		protocol: 'http',
                		repository: (pipelineParams.nexus_prod_repo),
                      	version: "${branch}-ims"
					)
                }
            }
          	stage("Uploading development WAR file to Nexus")
			{
            	when
				{
                	branch 'develop'
            	}
        		steps
				{
					nexusArtifactUploader(
						artifacts: [[artifactId: "${env.BRANCH_NAME}", classifier: '', file: "target/${projectArtifactId}-${projectVersion}.${artifactType}", type: "${artifactType}"],
							[artifactId: "${env.BRANCH_NAME}",classifier: '', file: "pom.xml", type: "pom" ],
                      		[artifactId: "${env.BRANCH_NAME}",classifier: '', file: "templates/templates.jar", type: "jar" ]],
                		credentialsId: 'd9f3ff8c-9dd2-4233-856f-db2921861c1a',
                		groupId: "${bitbucket_repo}",
                		nexusUrl: (pipelineParams.nexus_url),
                		nexusVersion: 'nexus3',
                		protocol: 'http',
                		repository: (pipelineParams.nexus_nonprod_repo),
                      	version: "${env.BRANCH_NAME}-ims"
					)
           		}
            }
          	stage('CleanWorkspace')
    		{
            	steps
              	{
        			cleanWs()
        			echo 'Cleaned workspace'
                }
            }
    	}
	}
}
