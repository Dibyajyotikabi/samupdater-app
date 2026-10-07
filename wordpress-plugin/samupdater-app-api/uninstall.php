<?php
/**
 * Removes the stored catalog when the plugin is deleted from the Plugins screen.
 */

defined( 'WP_UNINSTALL_PLUGIN' ) || exit;

delete_option( 'samupdater_app_catalog' );
