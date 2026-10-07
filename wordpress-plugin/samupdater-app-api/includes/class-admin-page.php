<?php
/**
 * Settings > Sam Updater App: a JSON editor for the catalog with validation.
 */

namespace SamUpdater\AppApi;

defined( 'ABSPATH' ) || exit;

final class Admin_Page {

	const SLUG           = 'samupdater-app-api';
	const CAPABILITY     = 'manage_options';
	const ACTION         = 'samupdater_app_save';
	const FIELD          = 'samupdater_catalog_json';
	const MAX_JSON_BYTES = 524288;
	const JSON_DEPTH     = 8;
	const DRAFT_SECONDS  = 600;

	public static function boot() {
		add_action( 'admin_menu', array( __CLASS__, 'menu' ) );
		add_action( 'admin_post_' . self::ACTION, array( __CLASS__, 'handle_save' ) );
	}

	public static function menu() {
		add_options_page( 'Sam Updater App', 'Sam Updater App', self::CAPABILITY, self::SLUG, array( __CLASS__, 'render' ) );
	}

	public static function handle_save() {
		if ( ! current_user_can( self::CAPABILITY ) ) {
			wp_die( 'You are not allowed to edit the app catalog.', '', array( 'response' => 403 ) );
		}
		check_admin_referer( self::ACTION );

		$raw    = isset( $_POST[ self::FIELD ] ) ? $_POST[ self::FIELD ] : '';
		$text   = is_string( $raw ) ? wp_unslash( $raw ) : ''; // phpcs:ignore WordPress.Security.ValidatedSanitizedInput.InputNotSanitized -- validated by Catalog_Schema below.
		$errors = self::validate_and_save( $text );

		if ( ! empty( $errors ) ) {
			// Keep what was typed so a typo doesn't wipe the edits.
			set_transient( self::draft_key(), array( 'text' => $text, 'errors' => $errors ), self::DRAFT_SECONDS );
		}
		wp_safe_redirect( add_query_arg( 'status', empty( $errors ) ? 'saved' : 'error', self::page_url() ) );
		exit;
	}

	/** @return string[] Problems found. Empty means the catalog was saved. */
	private static function validate_and_save( $text ) {
		if ( strlen( $text ) > self::MAX_JSON_BYTES ) {
			return array( 'The JSON is larger than 512 KB.' );
		}
		$decoded = json_decode( $text, true, self::JSON_DEPTH );
		if ( JSON_ERROR_NONE !== json_last_error() ) {
			return array( 'The JSON could not be read: ' . json_last_error_msg() . '.' );
		}

		$result = Catalog_Schema::sanitize( $decoded );
		if ( ! empty( $result['errors'] ) ) {
			return $result['errors'];
		}
		Catalog_Store::save( $result['catalog'] );
		return array();
	}

	public static function render() {
		if ( ! current_user_can( self::CAPABILITY ) ) {
			return;
		}
		$draft = get_transient( self::draft_key() );
		delete_transient( self::draft_key() );

		$errors = is_array( $draft ) ? (array) $draft['errors'] : array();
		$text   = is_array( $draft ) ? (string) $draft['text'] : self::pretty( Catalog_Store::get() );
		$status = isset( $_GET['status'] ) ? sanitize_key( wp_unslash( $_GET['status'] ) ) : ''; // phpcs:ignore WordPress.Security.NonceVerification.Recommended -- display only.
		$api    = rest_url( Rest_Controller::ROUTE_NAMESPACE . '/catalog' );
		?>
		<div class="wrap">
			<h1>Sam Updater App</h1>
			<?php self::notices( $status, $errors ); ?>
			<p>
				This catalog feeds the Android app: OS upgrade promises, One UI beta programs, upcoming updates and the CSC list.
				The app reads it from <a href="<?php echo esc_url( $api ); ?>" target="_blank" rel="noopener"><code><?php echo esc_html( $api ); ?></code></a>
				and caches it, so changes can take up to a few hours to reach every phone.
			</p>
			<form method="post" action="<?php echo esc_url( admin_url( 'admin-post.php' ) ); ?>">
				<input type="hidden" name="action" value="<?php echo esc_attr( self::ACTION ); ?>">
				<?php wp_nonce_field( self::ACTION ); ?>
				<textarea name="<?php echo esc_attr( self::FIELD ); ?>" rows="30" class="large-text code" spellcheck="false"><?php echo esc_textarea( $text ); ?></textarea>
				<?php submit_button( 'Validate and save' ); ?>
			</form>
			<?php self::field_help(); ?>
		</div>
		<?php
	}

	private static function notices( $status, array $errors ) {
		if ( 'saved' === $status && empty( $errors ) ) {
			echo '<div class="notice notice-success is-dismissible"><p>Catalog saved.</p></div>';
		}
		if ( empty( $errors ) ) {
			return;
		}
		echo '<div class="notice notice-error"><p>Nothing was saved. Fix these and try again:</p><ul>';
		foreach ( $errors as $error ) {
			echo '<li><code>' . esc_html( $error ) . '</code></li>';
		}
		echo '</ul></div>';
	}

	private static function field_help() {
		?>
		<h2>Fields</h2>
		<ul class="ul-disc">
			<li><code>devices</code>: modelPrefix (SM-S938), name, launchAndroid (15), osUpgrades (7), optional securityTier, securityUntil and rolloutCscs (["INS", "EUX"]).</li>
			<li><code>betaPrograms</code>: oneUi ("9"), status (<?php echo esc_html( implode( ', ', Catalog_Schema::BETA_STATUSES ) ); ?>), modelPrefixes, optional countries, latestBeta, noticeUrl (https) and updated.</li>
			<li><code>upcoming</code>: oneUi, stage (<?php echo esc_html( implode( ', ', Catalog_Schema::STAGES ) ); ?>), optional android, note and url (https).</li>
			<li><code>cscSuggestions</code>: code (INS) and country (India).</li>
		</ul>
		<p>The longest matching modelPrefix wins, so SM-S938 beats SM-S93. The updatedAt date is set for you on save.</p>
		<?php
	}

	private static function pretty( array $catalog ) {
		return (string) wp_json_encode( $catalog, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE );
	}

	private static function draft_key() {
		return 'samupdater_app_draft_' . get_current_user_id();
	}

	private static function page_url() {
		return admin_url( 'options-general.php?page=' . self::SLUG );
	}
}
