import { StyleSheet } from 'react-native';

const COLORS = {
    primary: '#0056A8', // Blue from Home/Login
    background: '#F5F7FB', // Light BG from Home/Login
    white: '#FFFFFF',
    textDark: '#333333',
    textGrey: '#666666',
    border: '#E0E4EC',
    danger: '#D32F2F',
    success: '#4CAF50',
    warning: '#F6B800', // Yellow from Home
};

export const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: COLORS.background,
        paddingHorizontal: 16,
        paddingTop: 16,
        paddingBottom: 24,
    },
    screenTitle: {
        marginBottom: 16,
        fontWeight: '700',
        textTransform: 'uppercase',
        color: COLORS.primary,
        fontSize: 20,
    },
    searchContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: COLORS.white,
        marginBottom: 16,
        paddingHorizontal: 12,
        paddingVertical: 12, // Slightly taller for better touch
        borderRadius: 12,
        borderWidth: 1,
        borderColor: COLORS.border,
        shadowColor: '#000',
        shadowOpacity: 0.05,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    searchIcon: {
        marginRight: 10,
    },
    searchInput: {
        flex: 1,
        fontSize: 16,
        color: COLORS.textDark,
    },
    listContent: {
        paddingBottom: 24,
    },
    // Card Styles
    card: {
        backgroundColor: COLORS.white,
        borderRadius: 12,
        padding: 16,
        marginBottom: 16,
        borderWidth: 1,
        borderColor: COLORS.border,
        shadowColor: '#000',
        shadowOpacity: 0.04,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 12,
    },
    eventTitle: {
        flex: 1,
        marginRight: 8,
        fontWeight: '700',
        fontSize: 18,
        color: COLORS.primary,
    },
    statusBadge: {
        backgroundColor: '#E3F2FD', // Very light blue
        borderRadius: 6,
        paddingHorizontal: 8,
        paddingVertical: 4,
        alignSelf: 'flex-start',
        borderWidth: 1,
        borderColor: '#BBDEFB',
    },
    statusText: {
        color: COLORS.primary,
        fontSize: 12,
        fontWeight: '600',
    },
    inlineStatusBadge: {
        borderRadius: 6,
        paddingHorizontal: 8,
        paddingVertical: 4,
        alignSelf: 'flex-start',
        marginTop: 4,
        backgroundColor: '#E3F2FD',
    },
    row: {
        flexDirection: 'row',
        marginBottom: 8, // Little more space
        alignItems: 'center',
    },
    label: {
        width: 80,
        fontWeight: '600',
        fontSize: 13,
        color: COLORS.textGrey,
    },
    value: {
        flex: 1,
        fontSize: 13,
        color: COLORS.textDark,
        fontWeight: '500',
    },
    footerButton: {
        marginTop: 16,
        alignSelf: 'stretch',
        backgroundColor: COLORS.white,
        borderWidth: 1,
        borderColor: COLORS.primary,
        borderRadius: 8,
        paddingVertical: 10,
        alignItems: 'center',
    },
    footerButtonText: {
        color: COLORS.primary,
        fontWeight: '600',
        fontSize: 14,
    },

    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
        paddingVertical: 40,
    },
    modalCard: {
        width: '100%',
        maxWidth: 420,
        maxHeight: '85%',
        borderRadius: 16,
        backgroundColor: COLORS.white,
        overflow: 'hidden',
        flexDirection: 'column',
        shadowColor: "#000",
        shadowOpacity: 0.1,
        shadowRadius: 10,
        shadowOffset: { width: 0, height: 4 },
        elevation: 5,
    },
    modalHeader: {
        backgroundColor: COLORS.background, // Light header instead of heavy blue
        paddingHorizontal: 20,
        paddingTop: 20,
        paddingBottom: 20,
        flexDirection: 'row',
        alignItems: 'center',
        borderBottomWidth: 1,
        borderBottomColor: COLORS.border,
    },
    modalImage: {
        width: 60,
        height: 60,
        marginRight: 16,
        borderRadius: 8,
        backgroundColor: COLORS.white,
    },
    modalTitle: {
        flex: 1,
        color: COLORS.primary, // Blue title
        fontWeight: '700',
        fontSize: 20,
        lineHeight: 24,
    },
    modalBody: {
        paddingHorizontal: 20,
        paddingVertical: 20,
        gap: 12,
    },
    sectionTitle: {
        fontWeight: '700',
        marginBottom: 4,
        color: COLORS.primary,
        fontSize: 14,
    },
    modalRow: {
        marginTop: 12,
    },

    // Buttons
    registerButton: {
        marginTop: 20,
        backgroundColor: COLORS.primary, // Standard Blue
        paddingVertical: 14,
        paddingHorizontal: 20,
        borderRadius: 10,
        alignItems: 'center',
        shadowColor: COLORS.primary,
        shadowOpacity: 0.3,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
    },
    registerButtonText: {
        color: COLORS.white,
        fontSize: 16,
        fontWeight: '600',
        letterSpacing: 0.5,
    },
    unregisterButton: {
        marginTop: 20,
        backgroundColor: COLORS.white,
        borderWidth: 1,
        borderColor: COLORS.danger, // Outlined red
        paddingVertical: 14,
        paddingHorizontal: 20,
        borderRadius: 10,
        alignItems: 'center',
    },
    unregisterButtonText: { // Make sure to use this for unregister
        color: COLORS.danger,
        fontSize: 16,
        fontWeight: '600',
    },
    modalCloseButton: {
        backgroundColor: COLORS.background,
        paddingVertical: 16,
        alignItems: 'center',
        borderTopWidth: 1,
        borderTopColor: COLORS.border,
    },
    modalCloseButtonText: {
        color: COLORS.textGrey,
        fontWeight: '600',
        fontSize: 15,
    },

    // Success View
    successContainer: {
        alignItems: 'center',
        paddingVertical: 30,
        paddingHorizontal: 20,
    },
    successTitle: {
        fontSize: 22,
        fontWeight: '700',
        color: COLORS.primary, // Blue instead of Green
        marginBottom: 12,
        textAlign: 'center',
    },
    successMessage: {
        fontSize: 16,
        textAlign: 'center',
        marginBottom: 32,
        lineHeight: 24,
        color: COLORS.textDark,
    },
    undoButton: {
        marginTop: 0,
        paddingVertical: 12,
        paddingHorizontal: 24,
        backgroundColor: COLORS.background,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: COLORS.border,
    },
    undoButtonText: {
        color: COLORS.textGrey,
        fontWeight: '600',
    },
    timerContainer: {
        marginTop: 24,
        width: '100%',
        height: 6,
        backgroundColor: COLORS.border,
        borderRadius: 3,
        overflow: 'hidden',
    },
    timerBar: {
        height: '100%',
        backgroundColor: COLORS.primary, // Blue timer
    },
    timerText: {
        textAlign: 'center',
        fontSize: 12,
        color: COLORS.textGrey,
        marginTop: 8,
    },

    // Util
    centered: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 24,
    },
    loadingText: {
        marginTop: 12,
        fontSize: 14,
        color: COLORS.textGrey,
    },
    errorText: {
        textAlign: 'center',
        color: COLORS.danger,
    },
    emptyText: {
        textAlign: 'center',
        color: COLORS.textGrey,
        marginTop: 20,
    },

    // Extra styles for [id].tsx
    debugBox: {
        marginTop: 12,
        padding: 10,
        borderWidth: 1,
        borderColor: '#d0d7de',
        borderRadius: 8,
        backgroundColor: '#f6f8fa',
    },
    debugTitle: {
        fontWeight: '700',
        marginBottom: 4,
    },
    debugItem: {
        fontSize: 13,
        color: '#4a5568',
    },
    modalLoadingContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 40,
    },
    modalLoadingText: {
        marginTop: 12,
        fontSize: 14,
        color: COLORS.textGrey,
    },
    modalErrorContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 20,
        paddingHorizontal: 20,
    },
    modalErrorText: {
        textAlign: 'center',
        color: COLORS.danger,
        fontSize: 16,
        fontWeight: '600',
        marginBottom: 8,
    },
    modalErrorHint: {
        textAlign: 'center',
        color: COLORS.textGrey,
        fontSize: 14,
    },
    successIcon: {
        fontSize: 48,
        marginBottom: 16,
        textAlign: 'center',
    },
    infoBox: {
        backgroundColor: COLORS.background,
        padding: 12,
        borderRadius: 8,
        marginTop: 16,
        borderWidth: 1,
        borderColor: COLORS.border,
        alignItems: 'center',
    },
    infoText: {
        color: COLORS.textGrey,
        fontSize: 14,
        textAlign: 'center',
        lineHeight: 20,
    },
});
