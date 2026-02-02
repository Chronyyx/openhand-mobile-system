import React, { useState } from 'react';
import { View, StyleSheet, Pressable, ActivityIndicator, Alert, StyleProp, ViewStyle, ImageStyle } from 'react-native';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import { Ionicons } from '@expo/vector-icons';
import { ThemedText } from './themed-text';
import { resolveUrl } from '../utils/api';
import { useTranslation } from 'react-i18next';

type ImageUploaderProps = {
    imageUrl: string | null | undefined;
    onUploadSuccess: (url: string) => void;
    uploadFunction: (uri: string, fileName?: string, mimeType?: string) => Promise<{ url: string | null }>;
    editable?: boolean;
    placeholderIcon?: keyof typeof Ionicons.glyphMap;
    containerStyle?: StyleProp<ViewStyle>;
    imageStyle?: StyleProp<ViewStyle>; // imageContainer is a View
};

export function ImageUploader({
    imageUrl,
    onUploadSuccess,
    uploadFunction,
    editable = true,
    placeholderIcon = 'image',
    containerStyle,
    imageStyle
}: ImageUploaderProps) {
    const { t } = useTranslation();
    const [isUploading, setIsUploading] = useState(false);

    const handlePickImage = async () => {
        if (!editable) return;

        const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (!permission.granted) {
            Alert.alert(t('common.permissions.title', 'Permission Required'), t('common.permissions.library', 'Please allow access to your photo library to upload images.'));
            return;
        }

        const result = await ImagePicker.launchImageLibraryAsync({
            mediaTypes: ImagePicker.MediaTypeOptions.Images,
            allowsEditing: true,
            aspect: [1, 1], // Profile uses [1,1]. Event logic might need flexibility.
            quality: 0.8,
        });

        if (result.canceled) {
            return;
        }

        const asset = result.assets[0];
        if (!asset?.uri) {
            Alert.alert(t('common.error'), t('common.errors.imageSelection'));
            return;
        }

        setIsUploading(true);
        try {
            // Reusing the exact logic pattern from ProfileScreen
            const response = await uploadFunction(
                asset.uri,
                asset.fileName ?? undefined,
                asset.mimeType ?? undefined
            );

            if (response.url) {
                onUploadSuccess(response.url);
            } else {
                Alert.alert(t('common.error'), t('common.errors.uploadFailed'));
            }
        } catch (error) {
            console.error('[ImageUploader] Upload failed', error);
            Alert.alert(t('common.error'), t('common.errors.uploadFailed'));
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <View style={[styles.container, containerStyle]}>
            <View style={[styles.imageContainer, imageStyle]}>
                {imageUrl ? (
                    <Image
                        source={{ uri: resolveUrl(imageUrl) }}
                        style={styles.image}
                        contentFit="cover"
                    />
                ) : (
                    <Ionicons name={placeholderIcon} size={40} color="#0056A8" />
                )}
            </View>

            {editable && (
                <Pressable
                    style={[styles.button, isUploading && styles.buttonDisabled]}
                    onPress={handlePickImage}
                    disabled={isUploading}
                    accessibilityRole="button"
                    accessibilityLabel={
                        imageUrl
                            ? t('common.actions.changePhoto', 'Change Photo')
                            : t('common.actions.addPhoto', 'Add Photo')
                    }
                    accessibilityHint={t('common.actions.changePhotoHint', 'Opens your photo library')}
                    accessibilityState={{ disabled: isUploading }}
                >
                    {isUploading ? (
                        <ActivityIndicator size="small" color="#FFFFFF" />
                    ) : (
                        <ThemedText style={styles.buttonText}>
                            {imageUrl ? t('common.actions.changePhoto', 'Change Photo') : t('common.actions.addPhoto', 'Add Photo')}
                        </ThemedText>
                    )}
                </Pressable>
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        gap: 16
    },
    imageContainer: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: '#EAF1FF',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: '#E0E6F0',
    },
    image: {
        width: '100%',
        height: '100%',
    },
    button: {
        backgroundColor: '#0056A8',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 16,
        minWidth: 140,
        alignItems: 'center',
    },
    buttonDisabled: {
        opacity: 0.6,
    },
    buttonText: {
        color: '#FFFFFF',
        fontWeight: '600',
        fontSize: 14,
    }
});
