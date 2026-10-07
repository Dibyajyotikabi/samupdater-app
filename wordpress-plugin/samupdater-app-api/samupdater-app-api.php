<?php
/**
 * Plugin Name: Sam Updater App API
 * Description: Read-only JSON catalog for the Sam Updater Android app. Holds OS upgrade promises, One UI beta programs, upcoming updates and CSC suggestions.
 * Version: 1.0.0
 * Requires at least: 6.0
 * Requires PHP: 7.4
 * Author: Dibyajyoti Kabi
 * License: GPL-2.0-or-later
 * Text Domain: samupdater-app-api
 */

namespace SamUpdater\AppApi;

defined( 'ABSPATH' ) || exit;

const VERSION = '1.0.0';
define( 'SAMUPDATER_APP_API_DIR', plugin_dir_path( __FILE__ ) );

require_once SAMUPDATER_APP_API_DIR . 'includes/class-catalog-schema.php';
require_once SAMUPDATER_APP_API_DIR . 'includes/class-catalog-store.php';
require_once SAMUPDATER_APP_API_DIR . 'includes/class-rest-controller.php';
require_once SAMUPDATER_APP_API_DIR . 'includes/class-admin-page.php';

register_activation_hook( __FILE__, array( Catalog_Store::class, 'install' ) );
add_action( 'rest_api_init', array( Rest_Controller::class, 'register' ) );

if ( is_admin() ) {
	Admin_Page::boot();
}
