def call(Map pipelineParams) {
 
  pipeline {
    options {
      ansiColor('xterm')
      timestamps()
      disableConcurrentBuilds()
    }
    environment {
      AWSCRED="platform_core_xlbot"
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
        yaml kubernetesPodTemplateNew(TF_VERSION: "${pipelineParams.TF_VERSION}")
        showRawYaml false
      }
    }
    // agent { label "${pipelineParams.AGENT_LABEL}" }
      stages{
        stage('Run Screener on All Accounts') {
          steps {
            script {
              container('terraformnew') {
                sshagent(credentials: ['xladmin']) {
                  withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
                    credentialsId: AWSCRED
                  ]]) {
                    sh '''
                      set +e

                      # Install dependencies
                      apk add --no-cache python3 py3-pip jq unzip git zip

                      # Clone repo with account list
                      cd /tmp
                      python3 -m venv .
                      source bin/activate
                      python3 -m pip install --upgrade pip
                      rm -rf service-screener-v2
                      #git clone https://github.com/aws-samples/service-screener-v2.git
                      git clone --branch release-20250530-1 --depth 1 git@gitrepo.xlaxiata.id:devops/service-screener-github.git service-screener-v2
                      cd service-screener-v2
                      pip install -r requirements.txt
                      python3 unzip_botocore_lambda_runtime.py
                      alias screener='python3 $(pwd)/main.py'
                      cd ..
                      git clone git@gitrepo.xlaxiata.id:devops/service-screener.git
                      cd service-screener

                      # Loop over accounts
                      tail -n +2 account_list.csv | while IFS=',' read -r _ ACCOUNT_ID _; do
                        ACCOUNT_ID=$(echo "$ACCOUNT_ID" | tr -d '\r' | tr -d '[:space:]')

                        # Skip if empty
                        [ -z "$ACCOUNT_ID" ] && continue

                        echo "===== Processing Account: '$ACCOUNT_ID' ====="
                        cd ..
                        cd service-screener-v2
                        
                        mkdir -p ~/.aws
                        cat > ~/.aws/credentials <<EOL
[master]
aws_access_key_id = $AWS_ACCESS_KEY_ID
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
EOL

                        cat > ~/.aws/config <<EOL
[profile acct-$ACCOUNT_ID]
role_arn = arn:aws:iam::$ACCOUNT_ID:role/tf-exec-admin
source_profile = master
region = ap-southeast-1
EOL

                        # Validate assumed role
                        aws sts get-caller-identity --profile acct-$ACCOUNT_ID
                        
                        # screener --regions ap-southeast-1,ap-southeast-3 --profile acct-$ACCOUNT_ID
                        # Retry screener if ConflictException occurs
                        MAX_RETRIES=5
                        RETRY_DELAY=10
                        COUNT=0

                        while [ $COUNT -lt $MAX_RETRIES ]; do
                          echo "[Attempt $((COUNT+1))] Running screener for acct-$ACCOUNT_ID..."
                          OUTPUT=$(screener --regions ap-southeast-1,ap-southeast-3 --profile acct-$ACCOUNT_ID 2>&1)
                          EXIT_CODE=$?

                          echo "$OUTPUT"

                          if echo "$OUTPUT" | grep -q "botocore.errorfactory.ConflictException"; then
                            echo "ConflictException detected. Retrying in $RETRY_DELAY seconds..."
                            COUNT=$((COUNT+1))
                            sleep $RETRY_DELAY
                          elif [ $EXIT_CODE -ne 0 ]; then
                            echo "Screener failed with unexpected error."
                            exit $EXIT_CODE
                          else
                            echo "Screener completed successfully for acct-$ACCOUNT_ID."
                            break
                          fi
                        done

                        if [ $COUNT -eq $MAX_RETRIES ]; then
                          echo "Failed after $MAX_RETRIES retries due to repeated ConflictExceptions."
                          exit 1
                        fi
                        # OUTPUT_DIR="/tmp/service-screener/service-screener/$ACCOUNT_ID"
                        # mkdir -p "$OUTPUT_DIR"
                        # cp /tmp/service-screener-v2/output.zip "$OUTPUT_DIR"
                        unzip -o /tmp/service-screener-v2/output.zip 

                      done
                      ls /tmp/service-screener-v2/aws/
                      mkdir -p ~/.aws
                      cat > ~/.aws/credentials <<EOL
[master]
aws_access_key_id = $AWS_ACCESS_KEY_ID
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
EOL

                      cat > ~/.aws/config <<EOL
[profile acct-916467617870]
role_arn = arn:aws:iam::916467617870:role/tf-exec-admin
source_profile = master
region = ap-southeast-1
EOL
                      #cd /tmp
                      #FINAL_ZIP="service-screener-results-$(date +%Y%m%d%H%M%S).zip"
                      #zip -r "$FINAL_ZIP" service-screener

                      # Upload the final ZIP to S3
                      aws sts get-caller-identity --profile acct-916467617870
                      # aws s3 cp "$FINAL_ZIP" s3://aws-service-screener-xlsmart/final-results/ --region ap-southeast-3 --profile acct-916467617870
                      # aws s3 sync /tmp/service-screener-v2/aws s3://service-screener-xlsmart/service-screener/ --region ap-southeast-1 --profile acct-916467617870
                      # DATE_PATH=$(date +%d%m%Y)
                      DATE_PATH=$(TZ=Asia/Jakarta date +%d%m%Y)
                      aws s3 sync /tmp/service-screener-v2/aws s3://service-screener-xlsmart/$DATE_PATH/ --region ap-southeast-1 --profile acct-916467617870

                    '''
                  }
                }
              }
            }
          }
        }
        // stage('debug loop') {
        //   steps {
        //     script {
        //       container('terraform') {
        //         sshagent(credentials: ['xladmin']) {
        //           withCredentials([[
        //             $class: 'AmazonWebServicesCredentialsBinding',
        //             accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        //             secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
        //             credentialsId: AWSCRED
        //           ]]) {
        //             sh '''
        //               set -e

        //               git clone git@gitrepo.xlaxiata.id:devops/service-screener.git
        //               cd service-screener

        //               # Loop over accounts
        //               tail -n +2 account_list.csv | while IFS=',' read -r _ ACCOUNT_ID _; do
        //                 ACCOUNT_ID=$(echo "$ACCOUNT_ID" | tr -d '\r' | tr -d '[:space:]')

        //                 # Skip if empty
        //                 [ -z "$ACCOUNT_ID" ] && continue

        //                 echo "===== Processing Account: '$ACCOUNT_ID' ====="
                        
        //               done
        //             '''
        //           }
        //         }
        //       }
        //     }
        //   }
        // }
//         stage('Run Screener on All Accounts pt 2') {
//           steps {
//             script {
//               container('terraform') {
//                 sshagent(credentials: ['xladmin']) {
//                   withCredentials([[
//                     $class: 'AmazonWebServicesCredentialsBinding',
//                     accessKeyVariable: 'AWS_ACCESS_KEY_ID',
//                     secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
//                     credentialsId: AWSCRED
//                   ]]) {
//                     sh '''
//                       cd /tmp
//                       cd service-screener-v2
//                       pip install -r requirements.txt
//                       python3 unzip_botocore_lambda_runtime.py
//                       alias screener='python3 $(pwd)/main.py'
//                       cd ..
//                       ls
//                       cd service-screener

//                       # Loop over accounts
//                       tail -n +2 account_list_2.csv | while IFS=',' read -r _ ACCOUNT_ID _; do
//                         echo "===== Processing Account: $ACCOUNT_ID ====="
//                         cd ..
//                         cd service-screener-v2
//                         mkdir -p ~/.aws
//                         cat > ~/.aws/credentials <<EOL
// [master]
// aws_access_key_id = $AWS_ACCESS_KEY_ID
// aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
// EOL

//                         cat > ~/.aws/config <<EOL
// [profile acct-$ACCOUNT_ID]
// role_arn = arn:aws:iam::$ACCOUNT_ID:role/tf-exec-admin
// source_profile = master
// region = ap-southeast-1
// EOL

//                         # Validate assumed role
//                         aws sts get-caller-identity --profile acct-$ACCOUNT_ID
                        
//                         screener --regions ap-southeast-1,ap-southeast-3 --profile acct-$ACCOUNT_ID
//                         # OUTPUT_DIR="/tmp/service-screener/service-screener/$ACCOUNT_ID"
//                         # mkdir -p "$OUTPUT_DIR"
//                         # cp /tmp/service-screener-v2/output.zip "$OUTPUT_DIR"
//                         unzip -o /tmp/service-screener-v2/output.zip 

//                       done
//                       ls /tmp/service-screener-v2/aws/
//                       mkdir -p ~/.aws
//                       cat > ~/.aws/credentials <<EOL
// [master]
// aws_access_key_id = $AWS_ACCESS_KEY_ID
// aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
// EOL

//                       cat > ~/.aws/config <<EOL
// [profile acct-916467617870]
// role_arn = arn:aws:iam::916467617870:role/tf-exec-admin
// source_profile = master
// region = ap-southeast-1
// EOL
//                       #cd /tmp
//                       #FINAL_ZIP="service-screener-results-$(date +%Y%m%d%H%M%S).zip"
//                       #zip -r "$FINAL_ZIP" service-screener

//                       # Upload the final ZIP to S3
//                       aws sts get-caller-identity --profile acct-916467617870
//                       # aws s3 cp "$FINAL_ZIP" s3://aws-service-screener-xlsmart/final-results/ --region ap-southeast-3 --profile acct-916467617870
//                       aws s3 sync /tmp/service-screener-v2/aws s3://service-screener-xlsmart/service-screener/ --region ap-southeast-1 --profile acct-916467617870
//                     '''
//                   }
//                 }
//               }
//             }
//           }
//         }

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

