def call(Map pipelineParams) {
  pipeline {
    options {
      ansiColor('xterm')
      timestamps()
      disableConcurrentBuilds()
    }
    parameters {
      choice(choices: 'nonprod\nprod', description: 'Choose your environment \n(nonprod or prod)', name: 'environment')
      string(defaultValue: 'terraform.tfvars', description: 'Enter your In variable definitions (.tfvars) file', name: 'tfvars')
      string(defaultValue: '', description: 'Enter your output name one at a time', name: 'output')
      string(name: 'action_resource', defaultValue: '', description: 'Enter the Terraform action and resource to perform (e.g. 1. terraform plan destroy "plan -destroy -target=module.ec2-testing -out=resource.tfplan") 2. terraform import "import module.ec2-testing.aws_instance.this i-0a83527fb54347609" :')
    }
    environment {
      AWSCRED="platform_core"
      REGION='ap-southeast-1'
      AWS_DEFAULT_REGION = 'ap-southeast-1'
      INFRACOST_API_KEY = ""
      GIT_SSH_COMMAND = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
    }
    agent {
      kubernetes {
        cloud 'kubernetes-green'
        inheritFrom 'kube-agent'
        namespace 'default'
        yaml kubernetesPodTemplate(TF_VERSION: "${pipelineParams.TF_VERSION}")
        showRawYaml false
      }
    }
    stages {
      stage('Checkout') {
        steps{
          script{
            // withCredentials([[
            //     $class           : 'AmazonWebServicesCredentialsBinding',
            //     accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            //     credentialsId    : AWSCRED,  // ID of credentials in Jenkins
            //     secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
            // ]]) {
            container('terraform') {
              sshagent(credentials: ['xladmin']) {
                def gitLogOutput1 = sh(script: "git log -n 1 -p -- prod",returnStdout: true).trim()
                def jumlah1 = (gitLogOutput1.split('/').size() - 1)/3
                def gitLogOutput = sh(script: "git log -n 1 --grep='prod' --format='%b'",returnStdout: true).trim()
                def jumlah = (gitLogOutput.split('/').size() - 1)/3
                def uniqueProjects = new HashSet(), list_project = new HashSet(), uniqueProjectsList = new HashSet()
                def projects = int
                for (int i = 1; i <= jumlah; i++) {
                  projects = 2 + (i-1)*3
                  if (jumlah1 == 4) {
                    list_project = gitLogOutput1.split('/')[projects]
                  }
                  else {
                    list_project = gitLogOutput.split('/')[projects]
                  }
                  uniqueProjects.add(list_project)
                    if ('policies' in uniqueProjects){
                      uniqueProjects.remove('policies')
                      uniqueProjects.add('0_global_project')
                    }
                }
                uniqueProjectsList = uniqueProjects.toList()
                if ('0_global_project' in uniqueProjectsList){
                  uniqueProjectsList.remove('0_global_project')
                  uniqueProjectsList.add(0,'0_global_project')
                }
                env.UNIQUE_PROJECTS = uniqueProjectsList
                echo "list project: ${UNIQUE_PROJECTS}"
                project_account_number = uniqueProjectsList.size()
                echo "jumlah project: ${project_account_number}"
                // env.jumlah_project = project_account_number
                // echo "${jumlah_project}"

                // for (int i = 1; i<=project_account_number; i++){
                //   def project = uniqueProjectsList[i-1]
                //   env["project${i}"] = project
                //   echo "project${i}: ${project}"
                // }
                // env.project_account = uniqueProjectsList[0]
                // echo "${project_account}"
                // def project1 = uniqueProjectsList[0]
                // echo project1
                // env.UNIQUE_PROJECTS = uniqueProjectsList.join(',')
                // echo "${UNIQUE_PROJECTS}"
              }
            }
            // }
          }
        }
      }    
      stage('Perform Terraform action') {
        when {
          beforeAgent true 
          expression { params.action_resource != '' }
        }
        steps {
          script {
            if (params.action_resource.toLowerCase().startsWith('plan')) {
              withCredentials([[
                                  $class           : 'AmazonWebServicesCredentialsBinding',
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                  credentialsId    : AWSCRED,  // ID of credentials in Jenkins
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]) {
                container('terraform') {
                  sshagent(credentials: ['xladmin']) {
                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi
                      terraform init -input=false
                      terraform ${action_resource}

                    '''
                    input message: "Are you sure you want to perform action '$action_resource' to the Terraform?",
                    ok: 'Yes',
                    submitter: 'deployer'

                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi

                      terraform apply -auto-approve resource.tfplan
                    '''
                  }
                }
              }
            } else if (params.action_resource.toLowerCase().startsWith('import')) {
                withCredentials([[
                                  $class           : 'AmazonWebServicesCredentialsBinding',
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                  credentialsId    : AWSCRED,  // ID of credentials in Jenkins
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]) {
                container('terraform') {
                  sshagent(credentials: ['xladmin']) {
                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi
                      terraform init -input=false
                      terraform ${action_resource}
                      terraform plan -out=resource.tfplan

                    '''

                    input message: "Are you sure you want to perform action '$action_resource' to the Terraform?",
                    ok: 'Yes',
                    submitter: 'deployer'

                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi

                      terraform apply -auto-approve resource.tfplan
                    '''
                }
              }
            }
          }
          else if (params.action_resource.toLowerCase().startsWith('state')) {
              withCredentials([[
                                  $class           : 'AmazonWebServicesCredentialsBinding',
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                  credentialsId    : AWSCRED,  // ID of credentials in Jenkins
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]) {
                container('terraform') {
                  sshagent(credentials: ['xladmin']) {

                    input message: "Are you sure you want to perform action '$action_resource' to the Terraform?",
                    ok: 'Yes',
                    submitter: 'deployer'

                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi
                      terraform init -input=false
                      terraform ${action_resource}
                    '''
                }
              }
            }
          }
          else if (action_resource != '' ) {
                          withCredentials([[
                                  $class           : 'AmazonWebServicesCredentialsBinding',
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                  credentialsId    : AWSCRED,  // ID of credentials in Jenkins
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]) {
                container('terraform') {
                  sshagent(credentials: ['xladmin']) {

                    input message: "Are you sure you want to perform action '$action_resource' to the Terraform?",
                    ok: 'Yes',
                    submitter: 'deployer'

                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi
                      terraform init -input=false
                      terraform ${action_resource}
                      terraform plan -out=resource.tfplan
                    '''

                    input message: "Are you sure you want to apply action '$action_resource' to the Terraform?",
                    ok: 'Yes',
                    submitter: 'deployer'

                    sh '''
                      if [ -f main.template ]
                      then
                        cp -R main.template main.tf
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            ACCOUNT_KEY=`grep account_id prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments prd.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            ACCOUNT_KEY=`grep account_id npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                            BUCKET_BACKEND=`grep environments npr.tfvars |awk -F" " '{gsub(/"/, "", $3); print $3}'`
                          ;;
                        esac
                        sed -i -e 's@ACCOUNT_KEY@'"$ACCOUNT_KEY"'@' main.tf
                        sed -i -e 's@BUCKET_BACKEND@'"$BUCKET_BACKEND"'@' main.tf
                      elif [ ! -d ${environment}  ]
                      then
                        cd prod/
                      else
                        cd ${environment}
                      fi
                      terraform apply -auto-approve resource.tfplan
                    '''
                }
              }
            }
          }
          else {
              echo "Invalid Terraform action or resource specified. Command: '$action_resource' "
          }

          }
        }
          post {
        always  {
          echo "Terraform action completed."
        }
      }
      }


      stage ('set env var') {
        when {
          beforeAgent true
          expression { params.action_resource == '' }
          anyOf {
            expression { env.gitlabActionType == "MERGE" }
            expression { env.gitlabActionType == "NOTE" }
            expression { env.gitlabActionType == "PUSH" }
          }
        }
        steps {
            container('python') {
                script {
                    // Set the API endpoint
                    def api_endpoint = 'http://tf-env-controller-service.prod.svc.cluster.local'

                    // Check the action type
                    if (env.gitlabActionType == 'MERGE' || env.gitlabActionType == 'NOTE') {
                        // Set environment variable for merge request
                        def mr_response = sh(
                            script: "curl -s -X POST '${api_endpoint}/set_env_var_mr/' -H 'Content-Type: application/json' -d '{\"project_id\": ${env.gitlabMergeRequestTargetProjectId}, \"mr_id\": ${env.gitlabMergeRequestIid}}'",
                            returnStdout: true
                        ).trim()
                        def mr_environment = readJSON(text: mr_response).environment
                        env.environment = mr_environment
                    }
                    if (env.gitlabActionType == 'PUSH') {
                        // Set environment variable for commit
                        def commit_response = sh(
                            script: "curl -s -X POST '${api_endpoint}/set_env_var_commit/' -H 'Content-Type: application/json' -d '{\"commit_id\": \"${env.GIT_COMMIT}\", \"gitlab_source_repo_http_url\": \"${env.gitlabSourceRepoHttpUrl}\"}'",
                            returnStdout: true
                        ).trim()
                        def commit_environment = readJSON(text: commit_response).environment
                        env.environment = commit_environment
                    }
                    // Print the value of the environment variable
                    sh script: 'echo ${environment}'
                    addGitLabMRComment(comment: env.environment)
                }
            }
        }

      }
      stage ('check env var') {
        when {
          beforeAgent true
          expression { params.action_resource == '' }
          anyOf {
            expression { env.gitlabTargetBranch == "master" }
            expression { env.CI == "true"}
            expression { env.GIT_BRANCH == "origin/master"}
          }
        }
        stages {
          stage ('Linting'){
            parallel {
              stage('tflint') {
                steps {
                  container('tflint') {
                    sh '''
                  if [ -f main.template ]
                  then
                    true
                  elif [ ! -d ${environment}  ]
                  then
                      cd prod/
                  else
                      cd ${environment}
                  fi
                  tflint --init
                  tflint
                  '''
                  }

                }
              }
              stage('terraform fmt') {
                steps {
                  catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE'){
                    echo 'If failed here, run <terraform fmt> in your workspace before commit'
                    container('terraform') {
                      sh '''
                  if [ -f main.template ]
                  then
                    true
                  elif [ ! -d ${environment}  ]
                  then
                      cd prod/
                  else
                      cd ${environment}
                  fi
                  terraform fmt -check -recursive
                  '''
                    }
                  }
                }
              }
              stage('tfsec') {
                steps {
                  container('tfsec') {
                    sh '''
                      if [ -f main.template ]
                      then
                        case ${environment} in
                          prod)
                            tfvars="prd.tfvars"
                            cp ${tfvars} terraform.tfvars
                          ;;
                          nonprod)
                            tfvars="npr.tfvars"
                            cp ${tfvars} terraform.tfvars
                          ;;
                        esac
                      elif [ ! -d ${environment}  ]
                      then
                          cd prod/
                      else
                          cd ${environment}
                      fi
                      tfsec --minimum-severity CRITICAL -e aws-ec2-no-public-egress-sgr,aws-vpc-no-public-egress-sgr,aws-eks-no-public-cluster-access,aws-eks-no-public-cluster-access-to-cidr,aws-elastic-search-enforce-https,aws-iam-no-policy-wildcards --exclude-path "modules" --exclude-downloaded-modules --tfvars-file ${tfvars} .
                      '''
                  }
                }
              }
              stage('update commit status - plan') {
                steps {
                  echo 'Building....'
                  updateGitlabCommitStatus name: 'plan', state: 'running'
                }
              }
              stage('terraform plan') {
                steps {
                  script{
                    withCredentials([[
                                       $class           : 'AmazonWebServicesCredentialsBinding',
                                       accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                       credentialsId    : AWSCRED,  // ID of credentials in Jenkins
                                       secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]) {
                    container('terraform') {
                      sshagent(credentials: ['xladmin']) {
                      // env.jumlah = jumlah_project
                      // echo jumlah
                      // for (int i = 1; i<=jumlah; i++){
                      //   def projectName = env["project${i}"]
                      //   echo "Value of ${projectName}"
                      //   // env["project_${i}"] = ${project${i}}
                      //   // echo "project_${i}"
                        sh '''
                        for variable in ${UNIQUE_PROJECTS}; do
                        pwd
                        ls
                        project=$(echo "$variable" | tr -d '[,]')
                        if [ "$project" == "0_global_project" ]; then
                          cp ./prod/variables.tf ./prod/$project/variables.tf
                        else
                          cp ./prod/main.tf "./prod/$project/main.tf"
                          cp ./prod/terraform.tfvars ./prod/$project/terraform.tfvars
                          cp ./prod/variables.tf ./prod/$project/variables.tf
                          sed -i "s]baseline/sso-manual-creation/terraform.tfstate]baseline/sso-manual-creation/prod/$project/terraform.tfstate]g" "./prod/$project/main.tf"
                        fi
                        ls ./prod/$project
                        cat ./prod/$project/main.tf
                        pwd
                        cd ./prod/$project && \
                        terraform init -input=false
                        terraform output ${output} -no-color
                        terraform plan -out=$project-terraform.tfplan -input=false -lock=false -var-file=${tfvars}
                        cd ..
                        cd ..
                        ls
                        done
                        '''
                      // }

                      }
                    }
                  }
                }
                }

              }
            }
          }

          stage('infracost') {
            when {
              beforeAgent true
              expression { params.action_resource == '' }
              anyOf {
                expression { env.gitlabActionType == "MERGE" }
                expression { env.gitlabActionType == "NOTE" }
              }
            }
            steps {
              container('infracost') {
                script {
                  unstash 'plan_json'
                  sh script: 'curl -fsSL https://raw.githubusercontent.com/infracost/infracost/master/scripts/install.sh | sh'
                  sh script: 'infracost diff --path plan.json'
                  env.diff_output = sh script: 'infracost diff --path plan.json ', returnStdout: true
                  addGitLabMRComment(comment: env.diff_output)
                }
              }

            }
          }

          stage('Apply') {
            when {
              beforeAgent true
              expression { params.action_resource == '' }
              not {
                expression { env.gitlabActionType == "MERGE" }
              }
            }
            stages {
              stage('Apply the Plan?'){
                when {
                beforeAgent true
                expression { params.action_resource == '' }
                  anyOf {
                    expression { env.gitlabActionType == "PUSH" }
                    expression { env.gitlabBranch == "master" }
                    expression { env.GIT_BRANCH == "origin/master"}
                  }
                }
                options {
                  timeout(time:35, unit: 'MINUTES')
                }
                steps{
                  script{
                    echo "\033[41m The environment is: ${environment} \033[0m"
                    echo "\033[31m The environment is: ${environment} \033[0m"
                    def userInput = input(id: 'confirm', message: 'Apply Terraform?', parameters: [ [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Apply terraform ?', name: 'confirm'] ])
                  }
                }
              }
              stage ('update commit status - apply') {
                when {
                  beforeAgent true
                  expression { params.action_resource == '' }
                  anyOf {
                    expression { env.gitlabActionType == "PUSH" }
                    expression { env.gitlabBranch == "master" }
                    expression { env.GIT_BRANCH == "origin/master"}
                  }
                }
                steps {
                  echo 'Building....'
                  updateGitlabCommitStatus name: 'apply', state: 'running'
                }
              }
              stage ('terraform apply') {
                when {
                  beforeAgent true
                  expression { params.action_resource == '' }
                  anyOf {
                    expression { env.gitlabActionType == "PUSH" }
                    expression { env.gitlabBranch == "master" }
                    expression { env.GIT_BRANCH == "origin/master"}
                  }
                }
                steps {
                  updateGitlabCommitStatus name: 'apply', state: 'running'
                  withCredentials([[
                                       $class: 'AmazonWebServicesCredentialsBinding',
                                       accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                       credentialsId: AWSCRED,  // ID of credentials in Jenkins
                                       secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'

                                   ]]){
                    container('terraform') {
                      sshagent (credentials: ['xladmin']){
                        sh '''
                        for variable in ${UNIQUE_PROJECTS}; do
                        ls
                        project=$(echo "$variable" | tr -d '[,]')
                        cd ./prod/$project && \
                        terraform apply -input=false "$project-terraform.tfplan"
                        terraform output -state=$project-terraform.tfplan
                        cd ..
                        cd ..
                        done
                        '''
                      }
                    }
                  }
                }
                post{
                  success {
                    echo '===== Apply Executed Successfully ====='
                    updateGitlabCommitStatus name: 'apply', state: 'success'
                  }
                  failure {
                    echo '===== Apply Failed ====='
                    updateGitlabCommitStatus name: 'apply', state: 'failed'
                  }
                }
              }
            }
          }
        }
      }
    }
    post{
      success {
        echo '===== Pipeline Executed Successfully ====='
        updateGitlabCommitStatus name: 'plan', state: 'success'
      }
      failure {
        echo '===== Pipeline Failed ====='
        updateGitlabCommitStatus name: 'plan', state: 'failed'
      }
    }
  }
}
