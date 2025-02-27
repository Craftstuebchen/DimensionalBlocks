package com.aaaaahhhhhhh.bananapuncher714.dimensional.block.library.implementation.v1_15_R1;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;

import com.aaaaahhhhhhh.bananapuncher714.dimensional.block.library.api.DBlock;
import com.aaaaahhhhhhh.bananapuncher714.dimensional.block.library.api.DInfo;
import com.aaaaahhhhhhh.bananapuncher714.dimensional.block.library.api.DState;
import com.aaaaahhhhhhh.bananapuncher714.dimensional.block.library.api.world.CollisionResultBlock;

import net.minecraft.server.v1_15_R1.Block;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.BlockStateList;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.EnumDirection;
import net.minecraft.server.v1_15_R1.EnumHand;
import net.minecraft.server.v1_15_R1.EnumInteractionResult;
import net.minecraft.server.v1_15_R1.EnumPistonReaction;
import net.minecraft.server.v1_15_R1.FluidType;
import net.minecraft.server.v1_15_R1.GeneratorAccess;
import net.minecraft.server.v1_15_R1.IBlockAccess;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.IRegistry;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.Material;
import net.minecraft.server.v1_15_R1.MaterialMapColor;
import net.minecraft.server.v1_15_R1.MinecraftKey;
import net.minecraft.server.v1_15_R1.MovingObjectPositionBlock;
import net.minecraft.server.v1_15_R1.VoxelShape;
import net.minecraft.server.v1_15_R1.VoxelShapeCollision;
import net.minecraft.server.v1_15_R1.World;
import net.minecraft.server.v1_15_R1.WorldServer;

public class BananaBlock extends Block {
    private static Field MATERIAL_ENUM_PISTON_REACTION;
    private static Field BLOCK_INFO_MATERIAL;
    private static Field BLOCK_INFO_STRENGTH;

    protected static Map< Long, DBlock > GLOBAL_BLOCK_MAP = new ConcurrentHashMap<>();

    static {
        try {
            MATERIAL_ENUM_PISTON_REACTION = Material.a.class.getDeclaredField( "a" );
            MATERIAL_ENUM_PISTON_REACTION.setAccessible( true );

            BLOCK_INFO_MATERIAL = Info.class.getDeclaredField( "a" );
            BLOCK_INFO_MATERIAL.setAccessible( true );

            BLOCK_INFO_STRENGTH = Info.class.getDeclaredField( "f" );
            BLOCK_INFO_STRENGTH.setAccessible( true );
        } catch ( NoSuchFieldException | SecurityException e ) {
            e.printStackTrace();
        }
    }

    private DBlock block;
    private NamespacedKey key;

    private Map< String, BananaState< ? > > states;

    public BananaBlock( DBlock block ) {
        // Construct the info from the block
        super( getInfoFrom( block ) );

        this.block = block;
        this.key = block.getKey();

        initializeStates( block );
    }

    // Expose these methods to the user

    @Override
    public boolean isOccluding( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition ) {
        if ( iblockaccess instanceof World ) {
            World world = ( World ) iblockaccess;
            BananaBlockData data = new BananaBlockData( iblockdata );
            Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

            return block.isOccluding( data, location );
        }

        return block.getInfo().isOccluding();
    }

    @Override
    public void a( IBlockData iblockdata, World world, BlockPosition blockposition, Entity entity ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

        block.onContact( data, location, entity.getBukkitEntity() );
    }

    @Override
    public void dropNaturally( IBlockData iblockdata, World world, BlockPosition blockposition, ItemStack itemstack ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

        block.dropNaturally( data, location, CraftItemStack.asBukkitCopy( itemstack ) );
    }

    @Override
    public void attack( IBlockData iblockdata, World world, BlockPosition blockposition, EntityHuman entityhuman ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

        block.attackBlock( data, location, entityhuman.getBukkitEntity() );
    }

    @Override
    public boolean a( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EntityTypes< ? > entitytypes ) {
        if ( iblockaccess instanceof World ) {
            World world = ( World ) iblockaccess;
            BananaBlockData data = new BananaBlockData( iblockdata );
            Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

            MinecraftKey id = entitytypes.h();
            NamespacedKey key = new NamespacedKey( id.getNamespace(), id.getKey() );

            return block.canEntitySpawnOn( data, location, key );
        }

        return block.getInfo().isCanMobsSpawnOn();
    }

    // dropNaturally

    @Override
    public int a( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection ) {
        // Power source
        if ( iblockaccess instanceof World ) {
            World world = ( World ) iblockaccess;
            BananaBlockData data = new BananaBlockData( iblockdata );
            Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );
            BlockFace face = BlockFace.valueOf( enumdirection.name() );

            return block.getPowerSourceLevel( data, location, face );
        }
        return super.a( iblockdata, iblockaccess, blockposition, enumdirection );
    }

    @Override
    public boolean isPowerSource( IBlockData iblockdata ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        return block.isPowerSource( data );
    }

    @Override
    public int a( IBlockData iblockdata, World world, BlockPosition blockposition ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

        return block.getComparatorLevel( data, location );
    }

    @Override
    public boolean isComplexRedstone( IBlockData iblockdata ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        return block.isComplexRedstone( data );
    }

    @Override
    public void doPhysics( IBlockData iblockdata, World world, BlockPosition blockposition, Block nmsBlock, BlockPosition blockposition1, boolean flag ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );

        Location activated = new Location( world.getWorld(), blockposition1.getX(), blockposition1.getY(), blockposition1.getZ() );

        block.doPhysics( data, location, activated );
    }

    @Override
    public void c( World world, BlockPosition position ) {
        Location location = new Location( world.getWorld(), position.getX(), position.getY(), position.getZ() );
        block.handleRain( location );
    }

    @Override
    public EnumPistonReaction getPushReaction( IBlockData iblockdata ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        return EnumPistonReaction.valueOf( block.getPistonReaction( data ).name() );
    }

    @Override
    public MaterialMapColor e( IBlockData iblockdata, IBlockAccess access, BlockPosition position ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        Color color;
        if ( access instanceof World ) {
            World world = ( World ) access;
            Location location = new Location( world.getWorld(), position.getX(), position.getY(), position.getZ() );
            color = block.getMapColor( data, location );
        } else {
            color = block.getInfo().getMapColor();
        }
        return computeNearest( color.getRed(), color.getGreen(), color.getBlue() );
    }

    @Override
    public float getDurability() {
        return block.getExplosionResistance();
    }

    @Override
    public boolean a( IBlockData iblockdata, FluidType fluidtype ) {
        BananaBlockData data = new BananaBlockData( iblockdata );
        MinecraftKey id = IRegistry.FLUID.getKey( fluidtype );
        NamespacedKey key = new NamespacedKey( id.getNamespace(), id.getKey() );
        return block.destroyedByFluid( data, key );
    }

    @Override
    public void stepOn( World world, BlockPosition position, Entity entity ) {
        Location location = new Location( world.getWorld(), position.getX(), position.getY(), position.getZ() );
        block.stepOn( location, entity.getBukkitEntity() );
    }

    // On player right click
    @Override
    public EnumInteractionResult interact( IBlockData iblockdata, World world, BlockPosition position, EntityHuman entityhuman, EnumHand enumhand, MovingObjectPositionBlock movingobjectpositionblock ) {
        Location location = new Location( world.getWorld(), position.getX(), position.getY(), position.getZ() );
        BananaBlockData data = new BananaBlockData( iblockdata );
        HumanEntity entity = entityhuman.getBukkitEntity();
        CollisionResultBlock collision = NMSHandler.getResultFrom( world, movingobjectpositionblock );

        return EnumInteractionResult.valueOf( block.interact( data, location, entity, enumhand == EnumHand.MAIN_HAND, collision ).name() );
    }

    // On projectile hit
    @Override
    public void a( World world, IBlockData iblockdata, MovingObjectPositionBlock movingobjectpositionblock, Entity entity ) {
        CollisionResultBlock collision = NMSHandler.getResultFrom( world, movingobjectpositionblock );
        CraftEntity ent = entity.getBukkitEntity();
        BananaBlockData data = new BananaBlockData( iblockdata );

        block.onProjectileHit( data, ent, collision );
    }

    @Override
    public void onPlace( IBlockData iblockdata, World world, BlockPosition position, IBlockData iblockdata1, boolean flag ) {
        super.onPlace( iblockdata, world, position, iblockdata1, flag );

        // More accurate to call this "onBlockDataChange"

        Location location = new Location( world.getWorld(), position.getX(), position.getY(), position.getZ() );
        BananaBlockData newData = new BananaBlockData( iblockdata );

        block.onDataUpdate( newData, location );
    }

    @Override
    public void postBreak( GeneratorAccess generatoraccess, BlockPosition blockposition, IBlockData iblockdata ) {
        CraftWorld world = generatoraccess.getMinecraftWorld().getWorld();
        Location location = new Location( world, blockposition.getX(), blockposition.getY(), blockposition.getZ() );
        BananaBlockData data = new BananaBlockData( iblockdata );

        block.postBreak( data, location );
    }

    @Override
    public void tick( IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, Random random ) {
        Location location = new Location( worldserver.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );
        BananaBlockData data = new BananaBlockData( iblockdata );
        block.tick( data, location, random );
    }

    @Override
    public IBlockData updateState( IBlockData iblockdata, EnumDirection enumdirection, IBlockData iblockdata1, GeneratorAccess generatoraccess, BlockPosition blockposition, BlockPosition blockposition1 ) {
        Location blockLoc = new Location( generatoraccess.getMinecraftWorld().getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );
        Location neighbor = new Location( generatoraccess.getMinecraftWorld().getWorld(), blockposition1.getX(), blockposition1.getY(), blockposition1.getZ() );

        BlockFace face = BlockFace.valueOf( enumdirection.name() );

        BananaBlockData data = new BananaBlockData( iblockdata );

        block.updateState( data, blockLoc, neighbor, face );

        return data.getData();
    }

    // The rest of the methods here are for internal use

    @Override
    public boolean c( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition ) {
        // Suffocation damage detection
        BananaBlockData data = new BananaBlockData( iblockdata );
        if ( iblockaccess instanceof World ) {
            World world = ( World ) iblockaccess;
            Location location = new Location( world.getWorld(), blockposition.getX(), blockposition.getY(), blockposition.getZ() );
            if ( !block.causesSuffocation( data, location ) ) {
                return false;
            }
        }

        IBlockData subData = NMSHandler.getFor( iblockdata );
        if ( subData == null ) {
            return super.c( iblockdata, iblockaccess, blockposition );
        }

        return subData.getBlock().c( subData, iblockaccess, blockposition );
    }

    @Override
    public int a( IBlockData iblockdata ) {
        // Light level emission
        BananaBlockData data = new BananaBlockData( iblockdata ).lock();

        IBlockData subData = ( ( CraftBlockData ) block.getClientBlock( data ) ).getState();

        // Register this state
        // Normally, this would have been done in the NMS handler, but might as well as do it here
        // until I can find a reason otherwise.
        iblockdata.c();
        Block.REGISTRY_ID.b( iblockdata );
        NMSHandler.setRegistryBlockId( iblockdata, subData );

        return subData.h();
    }	

    // Shape
    @Override
    public VoxelShape a( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision ) {
        IBlockData subData = NMSHandler.getFor( iblockdata );
        if ( subData == null ) {
            return super.a( iblockdata, iblockaccess, blockposition, voxelshapecollision );
        }
        // This is only a temporary fix until I can find a better solution
        // It doesn't appear to work nicely with all blocks, and the resulting
        // hitboxes of the surrounding blocks may be wrong or such
        return subData.a( iblockaccess, blockposition, voxelshapecollision );
    }

    // Collision shape
    @Override
    public VoxelShape b( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision ) {
        IBlockData subData = NMSHandler.getFor( iblockdata );
        if ( subData == null ) {
            return super.b( iblockdata, iblockaccess, blockposition, voxelshapecollision );
        }
        // No clue what this is used for
        return subData.b( iblockaccess, blockposition, voxelshapecollision );
    }
    
    // Occlusion shape
    @Override
    public VoxelShape i( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition ) {
        IBlockData subData = NMSHandler.getFor( iblockdata );
        if ( subData == null ) {
            return super.i( iblockdata, iblockaccess, blockposition );
        }
        // No clue what this is used for
        return subData.j( iblockaccess, blockposition );
    }
    
    // Interaction shape
    @Override
    public VoxelShape j( IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition ) {
        IBlockData subData = NMSHandler.getFor( iblockdata );
        if ( subData == null ) {
            return super.j( iblockdata, iblockaccess, blockposition );
        }
        // No clue what this is used for
        return subData.k( iblockaccess, blockposition );
    }

    @Override
    protected void a( BlockStateList.a< Block, IBlockData > blockstatelist ) {
        block = block == null ? GLOBAL_BLOCK_MAP.get( Thread.currentThread().getId() ) : block;
        initializeStates( block );
        blockstatelist.a( states.values().toArray( new BananaState[ states.size() ] ) );
    }

    public DBlock getBlock() {
        return block;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public < T extends Comparable< T > > T get( DState< T > state, IBlockData data ) {
        String id = state.getId();
        BananaState< ? > bState = states.get( id );
        if ( bState == null ) {
            throw new IllegalArgumentException( "State not part of this block!" );
        }
        DState< ? > dState = bState.getState();
        if ( dState.equals( state ) ) {
            return ( T ) data.get( bState );
        } else {
            throw new IllegalArgumentException( "Requested Invalid State" );
        }
    }

    public < T extends Comparable< T > > IBlockData set( DState< T > state, T value, IBlockData data ) {
        String id = state.getId();
        BananaState< T > bState = ( BananaState< T > ) states.get( id );
        if ( bState == null ) {
            throw new IllegalArgumentException( "State not part of this block!" );
        }
        DState< T > dState = bState.getState();
        if ( dState.equals( state ) ) {
            return data.set( bState, value );
        } else {
            throw new IllegalArgumentException( "Requested Invalid State" );
        }
    }

    public < T extends Comparable< T > > IBlockData increment( DState< T > state, IBlockData data ) {
        String id = state.getId();
        BananaState< T > bState = ( BananaState< T > ) states.get( id );
        if ( bState == null ) {
            throw new IllegalArgumentException( "State not part of this block!" );
        }
        DState< T > dState = bState.getState();
        if ( dState.equals( state ) ) {
            return data.a( bState );
        } else {
            throw new IllegalArgumentException( "Requested Invalid State" );
        }
    }

    protected void setAsDefault( IBlockData data ) {
        p( data );
    }

    private void initializeStates( DBlock block ) {
        // Dumb generics and their type erasure
        if ( states == null ) {
            states = new HashMap< String, BananaState< ? > >();
            for ( DState< ? > state : block.getStates() ) {
                BananaState< ? > bState = new BananaState( state );
                states.put( state.getId(), bState );
            }
        }
    }

    private static Info getInfoFrom( DBlock block ) {
        DInfo info = block.getInfo();

        Color color = info.getMapColor();
        Material.a material = new Material.a( computeNearest( color.getRed(), color.getGreen(), color.getBlue() ) );

        try {
            MATERIAL_ENUM_PISTON_REACTION.set( material, EnumPistonReaction.valueOf( info.getPistonReaction().name() ) );
        } catch ( IllegalArgumentException | IllegalAccessException e ) {
            e.printStackTrace();
        }

        CraftBlockData cData = ( CraftBlockData ) info.getBlockData();
        Info blockInfo = Info.a( cData.getState().getBlock() );
        try {
            BLOCK_INFO_MATERIAL.set( blockInfo, material.i() );
            BLOCK_INFO_STRENGTH.set( blockInfo, info.getExplosionResistance() );
        } catch ( IllegalArgumentException | IllegalAccessException e ) {
            e.printStackTrace();
        }

        return blockInfo;
    }

    private static MaterialMapColor computeNearest( int red, int green, int blue ) {
        float best_distance = Float.MAX_VALUE;
        MaterialMapColor best = null;
        MaterialMapColor val = null;
        for ( int i = 0; ( val = MaterialMapColor.a[ i ] ) != null && i < 64; i++ ) {
            int col = val.rgb;
            float distance = getDistance(red, green, blue, col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF);
            if ( best == null || distance < best_distance ) {
                best_distance = distance;
                best = val;
            }
        }
        return best;
    }

    private static float getDistance( int red, int green, int blue, int red2, int green2, int blue2 ) {
        float red_avg = ( red + red2 ) * .5f;
        int r = red - red2;
        int g = green - green2;
        int b = blue - blue2;
        float weight_red = 2.0f + red_avg * ( 1f / 256f );
        float weight_green = 4.0f;
        float weight_blue = 2.0f + ( 255.0f - red_avg ) * ( 1f / 256f );
        return weight_red * r * r + weight_green * g * g + weight_blue * b * b;
    }
}
