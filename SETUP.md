## Supabase Local Development Setup

This project uses a self-hosted Supabase environment managed via Docker Compose. Follow the instructions below to configure your environment variables, generate security keys, and spin up your local database stack.

## Prerequisites

Before running the setup script, ensure you have the following installed on your machine:
* [Docker Desktop](https://docs.docker.com/get-docker/) (Ensure the Docker daemon is active and running)
* A Bash/Sh compatible terminal (Linux, macOS, or Git Bash/WSL on Windows)

## Installation & Setup

To automate the initialization of configuration files, environment variables, and cryptographic keys required by Supabase, run:
* supabase-setup-script.sh

After the installation of the keys you might change the dashboard username and the password, etc. in the `.env` file