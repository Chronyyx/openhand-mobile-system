import { Client, IFrame, StompSubscription } from '@stomp/stompjs';

// Polyfill for TextEncoder/TextDecoder if necessary (handled in _layout.tsx usually)

type Listener = (message: any) => void;

class WebSocketService {
    private client: Client;
    private connected: boolean = false;
    // Map destination -> Set of listeners
    private listeners: Map<string, Set<Listener>> = new Map();
    // Map destination -> StompSubscription
    private subscriptions: Map<string, StompSubscription> = new Map();


    constructor() {
        // Strip '/api' from the end of the API URL if present to get the root base URL
        const baseUrl = process.env.EXPO_PUBLIC_API_URL?.replace(/\/api\/?$/, '');
        const brokerURL = baseUrl ? baseUrl.replace('http', 'ws') + '/ws' : undefined;
        this.client = new Client({
            brokerURL,
            forceBinaryWSFrames: true,
            appendMissingNULLonIncoming: true,
            onConnect: this.onConnect,
            onDisconnect: this.onDisconnect,
            onStompError: this.onStompError,
            // debug: (str) => console.log(str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });
    }

    public connect(token?: string) {
        if (!process.env.EXPO_PUBLIC_API_URL) return;

        if (token) {
            this.client.connectHeaders = {
                Authorization: `Bearer ${token}`
            };
        }

        if (!this.client.active) {
            this.client.activate();
        }
    }

    public disconnect() {
        if (this.client.active) {
            this.client.deactivate();
        }
        this.connected = false;
    }

    public subscribe(destination: string, callback: Listener): () => void {
        if (!this.listeners.has(destination)) {
            this.listeners.set(destination, new Set());
        }
        this.listeners.get(destination)?.add(callback);

        if (this.connected) {
            if (!this.subscriptions.has(destination)) {
                this.doSubscribe(destination);
            }
        } else {
            // Just add to listeners map, onConnect will handle standard subscription
        }

        return () => {
            const set = this.listeners.get(destination);
            if (set) {
                set.delete(callback);
                if (set.size === 0) {
                    this.listeners.delete(destination);
                    // Unsubscribe from STOMP to save resources? 
                    // Optional, but good practice if topic is dynamic
                    const sub = this.subscriptions.get(destination);
                    if (sub) {
                        sub.unsubscribe();
                        this.subscriptions.delete(destination);
                    }
                }
            }
        };
    }

    private doSubscribe(destination: string) {
        if (this.subscriptions.has(destination)) return;

        const sub = this.client.subscribe(destination, (message) => {
            try {
                const body = JSON.parse(message.body);
                // Notify all listeners
                const set = this.listeners.get(destination);
                set?.forEach(listener => listener(body));
            } catch (e) {
                console.error('Failed to parse websocket message', e);
            }
        });
        this.subscriptions.set(destination, sub);
    }

    private onConnect = (frame: IFrame) => {
        // console.log('WebSocket Connected');
        this.connected = true;

        // Process pending subscriptions
        this.listeners.forEach((set, destination) => {
            if (!this.subscriptions.has(destination)) {
                this.doSubscribe(destination);
            }
        });

        // Clear pending? They are now in listeners map via subscribe() call usually
        // But if subscribe was called while disconnected, we added to listeners AND pending.
        // Actually, logic above: subscribe adds to listeners always.
        // if connected -> doSubscribe.
        // if not -> do nothing (wait for onConnect).
        // onConnect -> iterate listeners and subscribe.
        // So pending array strictly not needed if we just iterate listeners.
    };

    private onDisconnect = () => {
        // console.log('WebSocket Disconnected');
        this.connected = false;
        this.subscriptions.clear();
    };

    private onStompError = (frame: IFrame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
    };
}

export const webSocketService = new WebSocketService();
