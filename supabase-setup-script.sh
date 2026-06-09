#!/bin/bash

echo "Setting up Supabase project..."

# Clone Supabase repo using sparse checkout
git clone --filter=blob:none --no-checkout https://github.com/supabase/supabase

cd supabase || exit

git sparse-checkout init --cone
git sparse-checkout set docker
git checkout master

cd ..

# Create project directory
mkdir -p supabase-project

# Copy files
cp -rf supabase/docker/* supabase-project
cp supabase/docker/.env.example supabase-project/.env

# Go to project folder
cd supabase-project || exit

# Pull docker images
docker compose pull

echo "Run: sh ./utils/generate-keys.sh"
echo ""
echo ""
echo "Supabase setup complete!"
echo "Run this to start Supabase:"
echo "cd supabase-project"
echo ""
echo ""
echo "docker compose up -d"