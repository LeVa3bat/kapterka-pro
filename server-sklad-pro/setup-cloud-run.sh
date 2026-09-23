#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="sklad-pro-a1ec0"
PROJECT_NUMBER="98775459607"
REGION="europe-central2"
POOL_ID="github-sklad-pro"
REPOSITORY="LeVa3bat/kapterka-pro"

DEPLOYER_NAME="github-sklad-deployer"
RUNTIME_NAME="sklad-pro-runtime"
BUILDER_NAME="sklad-pro-builder"

DEPLOYER_SA="${DEPLOYER_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
RUNTIME_SA="${RUNTIME_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
BUILDER_SA="${BUILDER_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud config set project "$PROJECT_ID" >/dev/null

BILLING_ENABLED="$(gcloud billing projects describe "$PROJECT_ID" --format='value(billingEnabled)' 2>/dev/null || true)"
if [[ "$BILLING_ENABLED" != "True" && "$BILLING_ENABLED" != "true" ]]; then
  echo "BILLING_NOT_ENABLED"
  echo "Cloud Run deployment is intentionally not enabled until billing is attached to $PROJECT_ID."
  exit 10
fi

gcloud services enable   run.googleapis.com   cloudbuild.googleapis.com   artifactregistry.googleapis.com   iamcredentials.googleapis.com   sts.googleapis.com   firestore.googleapis.com   identitytoolkit.googleapis.com   --project="$PROJECT_ID"

ensure_sa() {
  local name="$1"
  local display="$2"
  local email="${name}@${PROJECT_ID}.iam.gserviceaccount.com"
  if ! gcloud iam service-accounts describe "$email" --project="$PROJECT_ID" >/dev/null 2>&1; then
    gcloud iam service-accounts create "$name"       --project="$PROJECT_ID"       --display-name="$display"
  fi
}

ensure_sa "$DEPLOYER_NAME" "Sklad PRO GitHub backend deployer"
ensure_sa "$RUNTIME_NAME" "Sklad PRO Cloud Run runtime"
ensure_sa "$BUILDER_NAME" "Sklad PRO Cloud Build builder"

# GitHub may deploy this one dedicated Cloud Run backend and update its public-access setting.
for ROLE in   roles/run.admin   roles/serviceusage.serviceUsageConsumer
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID"     --member="serviceAccount:${DEPLOYER_SA}"     --role="$ROLE"     --quiet >/dev/null
done

# Dedicated source-build identity.
gcloud projects add-iam-policy-binding "$PROJECT_ID"   --member="serviceAccount:${BUILDER_SA}"   --role="roles/run.builder"   --quiet >/dev/null

# Runtime can only use Firestore and read Firebase Authentication user state.
for ROLE in   roles/datastore.user   roles/firebaseauth.viewer
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID"     --member="serviceAccount:${RUNTIME_SA}"     --role="$ROLE"     --quiet >/dev/null
done

# Deployer may attach the two dedicated service accounts to the build/service.
for TARGET_SA in "$RUNTIME_SA" "$BUILDER_SA"; do
  gcloud iam service-accounts add-iam-policy-binding "$TARGET_SA"     --project="$PROJECT_ID"     --member="serviceAccount:${DEPLOYER_SA}"     --role="roles/iam.serviceAccountUser"     --quiet >/dev/null
done

POOL_NAME="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_SA"   --project="$PROJECT_ID"   --role="roles/iam.workloadIdentityUser"   --member="principalSet://iam.googleapis.com/${POOL_NAME}/attribute.repository/${REPOSITORY}"   --quiet >/dev/null

echo "CLOUD_RUN_SETUP_READY"
echo "PROJECT=$PROJECT_ID"
echo "REGION=$REGION"
echo "DEPLOYER=$DEPLOYER_SA"
echo "RUNTIME=$RUNTIME_SA"
echo "BUILDER=$BUILDER_SA"
