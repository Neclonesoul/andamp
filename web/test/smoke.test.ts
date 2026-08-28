import { describe, expect, it } from 'vitest';
import { WebPlayerAdapter } from '../src/lib/player/web-adapter';
describe('web adapter',()=>{it('starts with protocol v1',async()=>{const a=new WebPlayerAdapter();expect((await a.getSnapshot()).protocolVersion).toBe(1)})});
